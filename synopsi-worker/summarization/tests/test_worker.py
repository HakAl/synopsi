import pytest
import sys
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from main import SummarizationWorker


class TestSummarizationWorkerInit:
    """Tests for SummarizationWorker initialization."""

    def test_init_stores_dependencies(self):
        mock_api_client = Mock()
        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        assert worker.api_client == mock_api_client
        assert worker.summarizer == mock_summarizer


class TestSummarizationWorkerRun:
    """Tests for run method."""

    def test_run_with_no_jobs(self):
        mock_api_client = Mock()
        mock_api_client.health_check.return_value = True
        mock_api_client.get_queued_jobs.return_value = []

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker.run()

        assert result['status'] == 'success'
        assert result['jobs_total'] == 0
        assert result['jobs_successful'] == 0
        assert result['jobs_failed'] == 0

    def test_run_with_successful_jobs(self):
        mock_api_client = Mock()
        mock_api_client.health_check.return_value = True
        mock_api_client.get_queued_jobs.return_value = [
            {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'},
            {'id': 2, 'articleId': 20, 'summaryType': 'DETAILED', 'summaryLength': 'MEDIUM'}
        ]
        mock_api_client.get_article.return_value = {
            'id': 10,
            'title': 'Test Article',
            'content': 'This is the article content to be summarized.'
        }
        mock_api_client.complete_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}
        mock_summarizer.summarize.return_value = ("This is a summary.", 50)

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker.run()

        assert result['status'] == 'success'
        assert result['jobs_total'] == 2
        assert result['jobs_successful'] == 2
        assert result['jobs_failed'] == 0
        assert mock_api_client.complete_job.call_count == 2

    def test_run_with_failed_jobs(self):
        mock_api_client = Mock()
        mock_api_client.health_check.return_value = True
        mock_api_client.get_queued_jobs.return_value = [
            {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'}
        ]
        mock_api_client.get_article.return_value = None  # Article not found
        mock_api_client.fail_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker.run()

        assert result['status'] == 'success'
        assert result['jobs_total'] == 1
        assert result['jobs_successful'] == 0
        assert result['jobs_failed'] == 1
        mock_api_client.fail_job.assert_called_once()

    def test_run_api_health_check_fails(self):
        mock_api_client = Mock()
        mock_api_client.health_check.return_value = False

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker.run()

        assert result['status'] == 'error'
        assert 'API unavailable' in result['error']

    def test_run_handles_summarization_error(self):
        mock_api_client = Mock()
        mock_api_client.health_check.return_value = True
        mock_api_client.get_queued_jobs.return_value = [
            {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'}
        ]
        mock_api_client.get_article.return_value = {
            'id': 10,
            'title': 'Test Article',
            'content': 'Content here.'
        }
        mock_api_client.fail_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}
        mock_summarizer.summarize.side_effect = RuntimeError("Model error")

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker.run()

        assert result['status'] == 'success'  # Worker itself succeeded
        assert result['jobs_failed'] == 1
        mock_api_client.fail_job.assert_called_once()


class TestSummarizationWorkerProcessJob:
    """Tests for _process_job method."""

    def test_process_job_success(self):
        mock_api_client = Mock()
        mock_api_client.get_article.return_value = {
            'id': 10,
            'title': 'Test Article',
            'content': 'This is the article content to be summarized.'
        }
        mock_api_client.complete_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}
        mock_summarizer.summarize.return_value = ("Summary text.", 30)

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        job = {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'}
        result = worker._process_job(job)

        assert result is True
        mock_api_client.complete_job.assert_called_once_with(
            job_id=1,
            summary_text="Summary text.",
            model_version="test-model",
            token_count=30
        )

    def test_process_job_article_not_found(self):
        mock_api_client = Mock()
        mock_api_client.get_article.return_value = None
        mock_api_client.fail_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        job = {'id': 1, 'articleId': 999, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'}
        result = worker._process_job(job)

        assert result is False
        mock_api_client.fail_job.assert_called_once()

    def test_process_job_empty_content(self):
        mock_api_client = Mock()
        mock_api_client.get_article.return_value = {
            'id': 10,
            'title': 'Test Article',
            'content': ''
        }
        mock_api_client.fail_job.return_value = True

        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        job = {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF', 'summaryLength': 'SHORT'}
        result = worker._process_job(job)

        assert result is False
        mock_api_client.fail_job.assert_called_once()


class TestSummarizationWorkerCleanup:
    """Tests for cleanup method."""

    def test_cleanup_closes_api_client(self):
        mock_api_client = Mock()
        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        worker.cleanup()

        mock_api_client.close.assert_called_once()


class TestSummarizationWorkerResultBuilding:
    """Tests for result building methods."""

    def test_build_result(self):
        mock_api_client = Mock()
        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        from datetime import datetime
        start_time = datetime.now()

        result = worker._build_result(start_time, 10, 8, 2)

        assert result['status'] == 'success'
        assert result['jobs_total'] == 10
        assert result['jobs_successful'] == 8
        assert result['jobs_failed'] == 2
        assert 'duration_seconds' in result
        assert 'start_time' in result
        assert 'end_time' in result

    def test_build_error_result(self):
        mock_api_client = Mock()
        mock_summarizer = Mock()
        mock_summarizer.get_model_version.return_value = "test-model"
        mock_summarizer.get_device_info.return_value = {'device': 'cpu'}

        worker = SummarizationWorker(
            api_client=mock_api_client,
            summarizer=mock_summarizer
        )

        result = worker._build_error_result("Test error message")

        assert result['status'] == 'error'
        assert result['error'] == "Test error message"
        assert result['jobs_total'] == 0
        assert result['jobs_successful'] == 0
        assert result['jobs_failed'] == 0
