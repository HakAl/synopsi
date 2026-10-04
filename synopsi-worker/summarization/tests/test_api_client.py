import pytest
import sys
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from api_client import SummaryAPIClient


class TestSummaryAPIClientInit:
    """Tests for SummaryAPIClient initialization."""

    @patch('api_client.SummaryAPIClient._create_session')
    @patch('api_client.SummaryAPIClient.login')
    def test_init_with_credentials_calls_login(self, mock_login, mock_session):
        mock_session.return_value = Mock()
        client = SummaryAPIClient(
            base_url="http://localhost:8080",
            username="user",
            password="pass"
        )
        mock_login.assert_called_once()

    @patch('api_client.SummaryAPIClient._create_session')
    def test_init_with_token_skips_login(self, mock_session):
        mock_session.return_value = Mock()
        mock_session.return_value.headers = {}
        client = SummaryAPIClient(
            base_url="http://localhost:8080",
            access_token="existing_token"
        )
        assert client.access_token == "existing_token"


class TestSummaryAPIClientLogin:
    """Tests for login functionality."""

    @patch('api_client.SummaryAPIClient._create_session')
    def test_login_success(self, mock_session):
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {'token': 'test_token'}

        mock_sess = Mock()
        mock_sess.post.return_value = mock_response
        mock_sess.headers = {}
        mock_session.return_value = mock_sess

        client = SummaryAPIClient(
            base_url="http://localhost:8080",
            username="user",
            password="pass"
        )

        assert client.access_token == 'test_token'

    @patch('api_client.SummaryAPIClient._create_session')
    def test_login_failure_raises(self, mock_session):
        mock_response = Mock()
        mock_response.status_code = 401
        mock_response.text = "Invalid credentials"

        mock_sess = Mock()
        mock_sess.post.return_value = mock_response
        mock_sess.headers = {}
        mock_session.return_value = mock_sess

        with pytest.raises(ValueError, match="Login failed"):
            SummaryAPIClient(
                base_url="http://localhost:8080",
                username="user",
                password="wrong"
            )


class TestSummaryAPIClientHealthCheck:
    """Tests for health_check method."""

    def test_health_check_success(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_sess.request.return_value = mock_response

            assert client.health_check() is True

    def test_health_check_failure(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 500
            mock_sess.request.return_value = mock_response

            assert client.health_check() is False


class TestSummaryAPIClientGetQueuedJobs:
    """Tests for get_queued_jobs method."""

    def test_get_queued_jobs_success(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.json.return_value = [
                {'id': 1, 'articleId': 10, 'summaryType': 'BRIEF'},
                {'id': 2, 'articleId': 20, 'summaryType': 'DETAILED'}
            ]
            mock_sess.request.return_value = mock_response

            jobs = client.get_queued_jobs()
            assert len(jobs) == 2
            assert jobs[0]['id'] == 1

    def test_get_queued_jobs_empty(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.json.return_value = []
            mock_sess.request.return_value = mock_response

            jobs = client.get_queued_jobs()
            assert jobs == []


class TestSummaryAPIClientGetArticle:
    """Tests for get_article method."""

    def test_get_article_success(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                'id': 1,
                'title': 'Test Article',
                'content': 'This is the content.'
            }
            mock_sess.request.return_value = mock_response

            article = client.get_article(1)
            assert article['title'] == 'Test Article'
            assert article['content'] == 'This is the content.'

    def test_get_article_not_found(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 404
            mock_sess.request.return_value = mock_response

            article = client.get_article(999)
            assert article is None


class TestSummaryAPIClientCompleteJob:
    """Tests for complete_job method."""

    def test_complete_job_success(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_sess.request.return_value = mock_response

            result = client.complete_job(
                job_id=1,
                summary_text="This is a summary.",
                model_version="test-model",
                token_count=50
            )
            assert result is True

            # Verify correct endpoint and params were used
            call_args = mock_sess.request.call_args
            assert 'summaries/callback/complete' in call_args[0][1]

    def test_complete_job_failure(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 500
            mock_response.text = "Server error"
            mock_sess.request.return_value = mock_response

            result = client.complete_job(
                job_id=1,
                summary_text="This is a summary.",
                model_version="test-model"
            )
            assert result is False


class TestSummaryAPIClientFailJob:
    """Tests for fail_job method."""

    def test_fail_job_success(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            mock_response = Mock()
            mock_response.status_code = 200
            mock_sess.request.return_value = mock_response

            result = client.fail_job(
                job_id=1,
                error_message="Something went wrong"
            )
            assert result is True


class TestSummaryAPIClientRetryOn401:
    """Tests for automatic retry on 401."""

    def test_retries_on_401(self):
        with patch('api_client.SummaryAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            # First call: return 401
            # After re-login: return 200
            mock_response_401 = Mock()
            mock_response_401.status_code = 401

            mock_response_200 = Mock()
            mock_response_200.status_code = 200
            mock_response_200.json.return_value = []

            mock_sess.request.side_effect = [mock_response_401, mock_response_200]

            # Mock login for re-authentication
            mock_login_response = Mock()
            mock_login_response.status_code = 200
            mock_login_response.json.return_value = {'token': 'new_token'}
            mock_sess.post.return_value = mock_login_response

            # Create client with token AND credentials so re-login works
            client = SummaryAPIClient(
                base_url="http://localhost:8080",
                access_token="old_token"
            )
            # Set credentials for re-login
            client.username = "user"
            client.password = "pass"

            # This should trigger retry after 401
            jobs = client.get_queued_jobs()

            # Should have called request twice (first 401, then success after re-login)
            assert mock_sess.request.call_count == 2
