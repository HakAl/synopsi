import pytest
from ingestion.main import IngestionWorker


class TestIngestionWorker:
    """Test suite for IngestionWorker."""

    def test_is_rss_feed_detection(self):
        """Test RSS feed detection logic."""
        # Create a minimal worker just for testing the method
        # We'll pass None for api_client since we're not using it
        from unittest.mock import Mock

        api_client = Mock()
        worker = IngestionWorker(api_client, [])

        # RSS feeds
        assert worker._is_rss_feed("https://example.com/rss") is True
        assert worker._is_rss_feed("https://example.com/feed") is True
        assert worker._is_rss_feed("https://example.com/atom.xml") is True
        assert worker._is_rss_feed("https://example.com/rss.xml") is True
        assert worker._is_rss_feed("https://example.com/feed.xml") is True

        # Regular web pages
        assert worker._is_rss_feed("https://example.com/article") is False
        assert worker._is_rss_feed("https://example.com/news/story") is False
        assert worker._is_rss_feed("https://example.com/") is False

        # Case insensitive
        assert worker._is_rss_feed("https://example.com/RSS") is True
        assert worker._is_rss_feed("https://example.com/FEED") is True

    def test_run_reuses_sources_and_feeds_and_survives_failed_articles(self):
        """
        Regression: a run against an existing database must reuse the source and
        feed instead of recreating them, and one failed article must not crash
        the run (the failure summary used a key the client never set).
        """
        from unittest.mock import Mock

        api_client = Mock()
        api_client.health_check.return_value = True
        api_client.create_source.return_value = {'id': 5}
        api_client.create_feed.return_value = {'id': 7}
        api_client.ensure_source.return_value = {'id': 5}
        api_client.ensure_feed.return_value = {'id': 7}
        api_client.create_articles_batch.return_value = {
            'successful': [{'id': 1}],
            'failed': [{
                'article_title': 'Broken',
                'feed_id': 7,
                'error_type': 'validation',
                'http_status': 400,
                'message': 'Invalid article data',
            }],
            'duplicates': [{'title': 'Seen before', 'originalUrl': 'https://example.com/old'}],
        }

        worker = IngestionWorker(api_client, ['https://example.com/rss'])
        worker.rss_fetcher = Mock()
        worker.rss_fetcher.fetch_feed.return_value = [
            {'title': 'Fresh', 'originalUrl': 'https://example.com/new'},
            {'title': 'Broken', 'originalUrl': 'https://example.com/broken'},
            {'title': 'Seen before', 'originalUrl': 'https://example.com/old'},
        ]

        result = worker.run()

        assert result['status'] == 'success'
        assert result['articles_posted_successfully'] == 1
        assert result['articles_failed'] == 1
        assert result['articles_duplicate'] == 1
        api_client.ensure_source.assert_called_once_with('https://example.com/rss')
        api_client.ensure_feed.assert_called_once_with('https://example.com/rss', 5)
        api_client.create_source.assert_not_called()
        api_client.create_feed.assert_not_called()
        posted = api_client.create_articles_batch.call_args.args[0]
        assert all(a['feedId'] == 7 for a in posted)
