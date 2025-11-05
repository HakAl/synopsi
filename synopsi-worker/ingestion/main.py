import logging
import sys
import os
from pathlib import Path
from typing import List, Dict
from datetime import datetime
from dotenv import load_dotenv

from fetchers.rss_fetcher import RSSFetcher
from fetchers.web_scraper import WebScraper
from api_client import SynopsiAPIClient

# Load .env from project root (parent directory of ingestion/)
env_path = Path(__file__).parent.parent / '.env'
load_dotenv(dotenv_path=env_path)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('ingestion.log')
    ]
)

logger = logging.getLogger(__name__)


class IngestionWorker:

    def __init__(self, api_client: SynopsiAPIClient, feed_urls: List[str]):
        self.api_client = api_client
        self.feed_urls = feed_urls
        self.rss_fetcher = RSSFetcher(user_agent="Synopsi-Ingestion/1.0")
        self.web_scraper = WebScraper(user_agent="Synopsi-Ingestion/1.0")
        self.created_feeds = []

        logger.info(f"Initialized ingestion worker with {len(feed_urls)} feed URLs")

    def run(self) -> dict:
        start_time = datetime.now()
        logger.info("=" * 60)
        logger.info("Starting ingestion run")
        logger.info("=" * 60)

        # Step 1: Health check
        if not self.api_client.health_check():
            logger.error("API health check failed. Aborting ingestion.")
            return self._build_error_result("API unavailable")

        # Step 2: For each feed URL, create source and feed
        logger.info(f"Creating sources and feeds for {len(self.feed_urls)} URLs")

        for feed_url in self.feed_urls:
            try:
                # Create source from feed URL
                logger.info(f"Creating source for feed URL: {feed_url}")
                source = self.api_client.create_source(feed_url)

                if not source or 'id' not in source:
                    logger.error(f"Failed to create source for {feed_url}")
                    continue

                source_id = source['id']
                logger.info(f"Created source ID: {source_id}")

                # Create feed with the source ID
                logger.info(f"Creating feed for URL: {feed_url}")
                feed = self.api_client.create_feed(feed_url, source_id)

                if not feed or 'id' not in feed:
                    logger.error(f"Failed to create feed for {feed_url}")
                    continue

                feed_id = feed['id']
                self.created_feeds.append({
                    'feedId': feed_id,
                    'feedUrl': feed_url,
                    'sourceId': source_id
                })
                logger.info(f"Created feed ID: {feed_id} for URL: {feed_url}")

            except Exception as e:
                logger.error(f"Error creating source/feed for {feed_url}: {e}", exc_info=True)
                continue

        if not self.created_feeds:
            logger.error("No feeds were created successfully. Aborting ingestion.")
            return self._build_error_result("No feeds created")

        logger.info(f"Successfully created {len(self.created_feeds)} feeds")

        # Step 3: Fetch articles from feeds (RSS or direct web pages)
        logger.info(f"Fetching articles from {len(self.created_feeds)} feeds")
        all_articles = []
        feed_stats = {}

        for feed_info in self.created_feeds:
            feed_url = feed_info['feedUrl']
            feed_id = feed_info['feedId']

            try:
                # Determine feed type and fetch articles
                if self._is_rss_feed(feed_url):
                    logger.info(f"Fetching RSS feed: {feed_url}")
                    articles = self.rss_fetcher.fetch_feed(feed_url)
                else:
                    logger.info(f"Scraping web page: {feed_url}")
                    # WebScraper.scrape_page() returns list of articles
                    articles = self.web_scraper.scrape_page(feed_url)

                # Add feedId to each article
                for article in articles:
                    article['feedId'] = feed_id

                all_articles.extend(articles)
                feed_stats[feed_url] = len(articles)
                logger.info(f"Fetched {len(articles)} articles from {feed_url} (feedId: {feed_id})")

            except Exception as e:
                logger.error(f"Failed to fetch feed {feed_url}: {e}")
                feed_stats[feed_url] = 0

        if not all_articles:
            logger.warning("No articles fetched from any feed")
            return self._build_result(start_time, feed_stats, 0, 0)

        logger.info(f"Total articles fetched: {len(all_articles)}")

        # Step 4: Post articles to API
        logger.info("Posting articles to API")
        results = self.api_client.create_articles_batch(all_articles)

        successful_count = len(results['successful'])
        failed_count = len(results['failed'])

        # Log failures
        if failed_count > 0:
            logger.warning(f"{failed_count} articles failed to post:")
            for failure in results['failed'][:5]:
                logger.warning(f"  - {failure['article'].get('title', 'Unknown')}: {failure['error']}")
            if failed_count > 5:
                logger.warning(f"  ... and {failed_count - 5} more")

        # Step 5: Build result summary
        result = self._build_result(start_time, feed_stats, successful_count, failed_count)

        logger.info("=" * 60)
        logger.info(f"Ingestion run completed in {result['duration_seconds']}s")
        logger.info(f"Feeds created: {len(self.created_feeds)}")
        logger.info(f"Articles posted: {successful_count}/{len(all_articles)}")
        logger.info("=" * 60)

        return result

    def _build_result(
        self,
        start_time: datetime,
        feed_stats: dict,
        successful: int,
        failed: int
    ) -> dict:
        """Build result summary dictionary."""
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()

        return {
            'status': 'success',
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'duration_seconds': round(duration, 2),
            'feeds_created': len(self.created_feeds),
            'feeds_processed': len(self.feed_urls),
            'feed_stats': feed_stats,
            'articles_fetched': sum(feed_stats.values()),
            'articles_posted_successfully': successful,
            'articles_failed': failed
        }

    def _build_error_result(self, error_message: str) -> dict:
        """Build error result dictionary."""
        return {
            'status': 'error',
            'error': error_message,
            'feeds_created': len(self.created_feeds),
            'articles_posted_successfully': 0,
            'articles_failed': 0
        }
    
    def _is_rss_feed(self, url: str) -> bool:
        """
        Determine if a URL is an RSS/Atom feed or a regular web page.
        Checks for common RSS indicators in the URL.

        Args:
            url: URL to check

        Returns:
            True if URL appears to be an RSS feed, False otherwise
        """
        url_lower = url.lower()

        # Common RSS/Atom indicators
        rss_indicators = [
            '/rss',
            '/feed',
            '/atom',
            '.rss',
            '.xml',
            'rss.xml',
            'feed.xml',
            'atom.xml',
        ]

        return any(indicator in url_lower for indicator in rss_indicators)

    def cleanup(self):
        """Cleanup resources."""
        self.api_client.close()
        self.web_scraper.close()
        logger.info("Cleanup complete")


def load_feed_urls_from_env() -> List[str]:
    """
    Load RSS feed URLs from environment variable.
    Format: Comma-separated list of URLs
    
    Returns:
        List of feed URLs
    """
    feeds_env = os.getenv('RSS_FEED_URLS', '')
    
    if not feeds_env:
        logger.warning("RSS_FEED_URLS environment variable not set, using defaults")
        return []

    feeds = [url.strip() for url in feeds_env.split(',') if url.strip()]
    
    if not feeds:
        logger.warning("No valid feed URLs found")
    
    return feeds


def main():
    """
    Main entry point for ingestion worker.
    Reads configuration from environment variables.
    """
    # Load configuration from environment
    api_base_url = os.getenv('API_BASE_URL', 'http://localhost:8080')
    api_username = os.getenv('API_USERNAME')
    api_password = os.getenv('API_PASSWORD')
    feed_urls = load_feed_urls_from_env()

    logger.info(f"Configuration:")
    logger.info(f"  API Base URL: {api_base_url}")
    logger.info(f"  API Username: {api_username}")
    logger.info(f"  Feed URLs: {len(feed_urls)} configured")

    # Validate configuration
    if not api_username or not api_password:
        logger.error("API_USERNAME and API_PASSWORD must be set in .env")
        sys.exit(1)
    
    if not feed_urls:
        logger.error("No RSS feed URLs configured")
        sys.exit(1)

    # Create API client with authentication
    try:
        api_client = SynopsiAPIClient(
            base_url=api_base_url,
            username=api_username,
            password=api_password
        )
    except Exception as e:
        logger.error(f"Failed to initialize API client: {e}")
        sys.exit(1)

    # Create worker with authenticated client
    worker = IngestionWorker(
        api_client=api_client,
        feed_urls=feed_urls
    )
    
    try:
        result = worker.run()
        
        # Exit with appropriate code
        if result['status'] == 'error':
            logger.error("Ingestion failed")
            sys.exit(1)
        elif result['articles_failed'] > 0:
            logger.warning("Ingestion completed with some failures")
            sys.exit(0)  # Still exit 0 if some articles succeeded
        else:
            logger.info("Ingestion completed successfully")
            sys.exit(0)
            
    except KeyboardInterrupt:
        logger.info("Ingestion interrupted by user")
        sys.exit(130)
    except Exception as e:
        logger.error(f"Unexpected error during ingestion: {e}", exc_info=True)
        sys.exit(1)
    finally:
        worker.cleanup()


if __name__ == '__main__':
    main()