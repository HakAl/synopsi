import pytest
import sys
from pathlib import Path
from unittest.mock import Mock, patch
import requests

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from api_client import SynopsiAPIClient, ArticleCreationError


class TestArticleCreationBatch:
    """Tests for batch article creation with error categorization."""

    def test_batch_success_all_articles(self):
        """Happy path: all articles created successfully."""
        with patch('api_client.SynopsiAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SynopsiAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            # Mock successful responses for all articles
            mock_response = Mock()
            mock_response.status_code = 201
            mock_response.json.return_value = {'id': 1}
            mock_sess.request.return_value = mock_response

            articles = [
                {'title': 'Article 1', 'originalUrl': 'http://example.com/1', 'feedId': 1},
                {'title': 'Article 2', 'originalUrl': 'http://example.com/2', 'feedId': 1}
            ]

            result = client.create_articles_batch(articles)

            assert len(result['successful']) == 2
            assert len(result['failed']) == 0

    def test_batch_network_error_categorization(self):
        """Error case: network errors are categorized correctly."""
        with patch('api_client.SynopsiAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SynopsiAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            # Mock network error
            mock_sess.request.side_effect = requests.exceptions.ConnectionError("Connection refused")

            articles = [
                {'title': 'Article 1', 'originalUrl': 'http://example.com/1', 'feedId': 1}
            ]

            result = client.create_articles_batch(articles)

            assert len(result['successful']) == 0
            assert len(result['failed']) == 1

            error = result['failed'][0]
            assert isinstance(error, dict)
            assert error['error_type'] == "network"
            assert error['article_title'] == "Article 1"

    def test_batch_validation_error_categorization(self):
        """Edge case: HTTP 4xx errors are categorized as validation errors."""
        with patch('api_client.SynopsiAPIClient._create_session') as mock_session:
            mock_sess = Mock()
            mock_sess.headers = {}
            mock_session.return_value = mock_sess

            client = SynopsiAPIClient(
                base_url="http://localhost:8080",
                access_token="token"
            )

            # Mock 400 validation error
            mock_response = Mock()
            mock_response.status_code = 400
            mock_response.text = "Missing required field"
            mock_sess.request.return_value = mock_response

            articles = [
                {'title': 'Invalid Article', 'originalUrl': 'http://bad-url.com', 'feedId': 1}
            ]

            result = client.create_articles_batch(articles)

            assert len(result['successful']) == 0
            assert len(result['failed']) == 1

            error = result['failed'][0]
            assert isinstance(error, dict)
            assert error['error_type'] == "validation"
            # ValueError doesn't have http_status since it's caught before response processing
            assert error['http_status'] is None
            assert error['article_title'] == "Invalid Article"
            assert "Invalid article data" in error['message']
