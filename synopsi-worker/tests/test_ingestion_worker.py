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
