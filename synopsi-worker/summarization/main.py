import logging
import sys
import os
from pathlib import Path
from typing import Dict, List
from datetime import datetime
from dotenv import load_dotenv

from .api_client import SummaryAPIClient
from .summarizer import Summarizer, DEFAULT_MODEL
from .preprocessor import preprocess_for_summary

# Load .env from project root (parent directory of summarization/)
env_path = Path(__file__).parent.parent / '.env'
load_dotenv(dotenv_path=env_path)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('summarization.log')
    ]
)

logger = logging.getLogger(__name__)


class SummarizationWorker:
    """
    Worker that processes summary jobs from the API queue.
    Fetches queued jobs, generates summaries, and reports results.
    """

    def __init__(self, api_client: SummaryAPIClient, summarizer: Summarizer):
        """
        Initialize the summarization worker.

        Args:
            api_client: API client for communicating with Synopsi API
            summarizer: Summarizer instance for generating summaries
        """
        self.api_client = api_client
        self.summarizer = summarizer

        logger.info("Initialized summarization worker")
        logger.info(f"Model: {summarizer.get_model_version()}")
        logger.info(f"Device info: {summarizer.get_device_info()}")

    def run(self) -> Dict:
        """
        Main processing loop.
        Fetches queued jobs, processes each one, and reports results.

        Returns:
            Result dictionary with processing statistics
        """
        start_time = datetime.now()
        logger.info("=" * 60)
        logger.info("Starting summarization run")
        logger.info("=" * 60)

        # Step 1: Health check
        if not self.api_client.health_check():
            logger.error("API health check failed. Aborting.")
            return self._build_error_result("API unavailable")

        # Step 2: Fetch queued jobs
        jobs = self.api_client.get_queued_jobs()

        if not jobs:
            logger.info("No queued jobs found")
            return self._build_result(start_time, 0, 0, 0)

        logger.info(f"Found {len(jobs)} queued jobs")

        # Step 3: Process each job
        successful = 0
        failed = 0

        for job in jobs:
            try:
                if self._process_job(job):
                    successful += 1
                else:
                    failed += 1
            except Exception as e:
                logger.error(f"Unexpected error processing job: {e}", exc_info=True)
                failed += 1

        # Step 4: Build result summary
        result = self._build_result(start_time, len(jobs), successful, failed)

        logger.info("=" * 60)
        logger.info(f"Summarization run completed in {result['duration_seconds']}s")
        logger.info(f"Jobs processed: {len(jobs)}, Success: {successful}, Failed: {failed}")
        logger.info("=" * 60)

        return result

    def _process_job(self, job: Dict) -> bool:
        """
        Process a single summary job.

        Args:
            job: Job dictionary from API

        Returns:
            True if successful, False otherwise
        """
        job_id = job.get('id')
        article_id = job.get('articleId')
        summary_type = job.get('summaryType', 'BRIEF')
        summary_length = job.get('summaryLength', 'MEDIUM')

        logger.info(f"Processing job {job_id} (article {article_id}, type={summary_type}, length={summary_length})")

        try:
            # Step 1: Fetch article content
            article = self.api_client.get_article(article_id)

            if not article:
                error_msg = f"Article {article_id} not found"
                logger.error(error_msg)
                self.api_client.fail_job(job_id, error_msg)
                return False

            content = article.get('content', '')
            title = article.get('title', 'Unknown')

            if not content:
                error_msg = f"Article {article_id} has no content"
                logger.error(error_msg)
                self.api_client.fail_job(job_id, error_msg)
                return False

            logger.debug(f"Article '{title}' has {len(content)} characters")

            # Step 2: Preprocess content
            preprocessed = preprocess_for_summary(content, max_tokens=1024)

            if not preprocessed:
                error_msg = "Content empty after preprocessing"
                logger.error(error_msg)
                self.api_client.fail_job(job_id, error_msg)
                return False

            logger.debug(f"Preprocessed to {len(preprocessed)} characters")

            # Step 3: Generate summary
            summary_text, token_count = self.summarizer.summarize(
                preprocessed,
                summary_type=summary_type,
                summary_length=summary_length
            )

            if not summary_text:
                error_msg = "Generated summary is empty"
                logger.error(error_msg)
                self.api_client.fail_job(job_id, error_msg)
                return False

            logger.info(f"Generated summary for job {job_id}: {len(summary_text)} chars")

            # Step 4: Report success
            model_version = self.summarizer.get_model_version()
            success = self.api_client.complete_job(
                job_id=job_id,
                summary_text=summary_text,
                model_version=model_version,
                token_count=token_count
            )

            if success:
                logger.info(f"Job {job_id} completed successfully")
                return True
            else:
                logger.error(f"Failed to report completion for job {job_id}")
                return False

        except ValueError as e:
            error_msg = f"Validation error: {str(e)}"
            logger.error(f"Job {job_id} failed: {error_msg}")
            self.api_client.fail_job(job_id, error_msg)
            return False

        except RuntimeError as e:
            error_msg = f"Runtime error: {str(e)}"
            logger.error(f"Job {job_id} failed: {error_msg}")
            self.api_client.fail_job(job_id, error_msg)
            return False

        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            logger.error(f"Job {job_id} failed: {error_msg}", exc_info=True)
            self.api_client.fail_job(job_id, error_msg)
            return False

    def _build_result(
        self,
        start_time: datetime,
        total: int,
        successful: int,
        failed: int
    ) -> Dict:
        """Build result summary dictionary."""
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()

        return {
            'status': 'success',
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'duration_seconds': round(duration, 2),
            'jobs_total': total,
            'jobs_successful': successful,
            'jobs_failed': failed
        }

    def _build_error_result(self, error_message: str) -> Dict:
        """Build error result dictionary."""
        return {
            'status': 'error',
            'error': error_message,
            'jobs_total': 0,
            'jobs_successful': 0,
            'jobs_failed': 0
        }

    def cleanup(self):
        """Cleanup resources."""
        self.api_client.close()
        logger.info("Cleanup complete")


def main():
    """
    Main entry point for summarization worker.
    Reads configuration from environment variables.
    """
    # Load configuration from environment
    api_base_url = os.getenv('API_BASE_URL', 'http://localhost:8080')
    api_username = os.getenv('API_USERNAME')
    api_password = os.getenv('API_PASSWORD')
    model_name = os.getenv('MODEL_NAME', DEFAULT_MODEL)

    logger.info("Configuration:")
    logger.info(f"  API Base URL: {api_base_url}")
    logger.info(f"  API Username: {api_username}")
    logger.info(f"  Model: {model_name}")

    # Validate configuration
    if not api_username or not api_password:
        logger.error("API_USERNAME and API_PASSWORD must be set in .env")
        sys.exit(1)

    # Initialize API client
    try:
        logger.info("Initializing API client...")
        api_client = SummaryAPIClient(
            base_url=api_base_url,
            username=api_username,
            password=api_password
        )
    except Exception as e:
        logger.error(f"Failed to initialize API client: {e}")
        sys.exit(1)

    # Initialize summarizer (this loads the ML model)
    try:
        logger.info("Loading summarization model (this may take a moment)...")
        summarizer = Summarizer(model_name=model_name)
    except Exception as e:
        logger.error(f"Failed to initialize summarizer: {e}")
        api_client.close()
        sys.exit(1)

    # Create worker
    worker = SummarizationWorker(
        api_client=api_client,
        summarizer=summarizer
    )

    try:
        result = worker.run()

        # Exit with appropriate code
        if result['status'] == 'error':
            logger.error("Summarization run failed")
            sys.exit(1)
        elif result['jobs_failed'] > 0 and result['jobs_successful'] == 0:
            logger.error("All jobs failed")
            sys.exit(1)
        elif result['jobs_failed'] > 0:
            logger.warning("Summarization completed with some failures")
            sys.exit(0)
        else:
            logger.info("Summarization completed successfully")
            sys.exit(0)

    except KeyboardInterrupt:
        logger.info("Summarization interrupted by user")
        sys.exit(130)
    except Exception as e:
        logger.error(f"Unexpected error during summarization: {e}", exc_info=True)
        sys.exit(1)
    finally:
        worker.cleanup()


if __name__ == '__main__':
    main()
