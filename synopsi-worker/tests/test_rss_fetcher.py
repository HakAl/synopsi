import pytest
from ingestion.fetchers.rss_fetcher import RSSFetcher


class TestRSSFetcher:
    """Test suite for RSSFetcher."""

    @pytest.fixture
    def fetcher(self):
        """Create RSSFetcher instance for testing."""
        return RSSFetcher(user_agent="Synopsi-Test/1.0")

    @pytest.fixture
    def required_fields(self):
        """List of required fields in article dictionary."""
        return [
            'title',
            'originalUrl',
            'content',
            'description',
            'author',
            'publicationDate',
            'source',
            'feedTitle',
            'language'
        ]

    def test_fetch_hacker_news_rss(self, fetcher, required_fields):
        """Test fetching Hacker News RSS feed."""
        feed_url = "https://news.ycombinator.com/rss"
        articles = fetcher.fetch_feed(feed_url)

        # Should return articles
        assert len(articles) > 0, "Should return at least one article"

        # Check first article has all required fields
        article = articles[0]
        for field in required_fields:
            assert field in article, f"Article should have '{field}' field"

        # Verify some fields are not empty
        assert article['title'], "Title should not be empty"
        assert article['originalUrl'], "Original URL should not be empty"

    def test_invalid_feed_url(self, fetcher):
        """Test handling of invalid feed URL."""
        with pytest.raises(ValueError):
            fetcher.fetch_feed("not-a-valid-url")

    def test_nonexistent_feed(self, fetcher):
        """Test handling of non-existent feed."""
        with pytest.raises(ValueError):
            fetcher.fetch_feed("https://example.com/nonexistent-feed.xml")

    def test_is_valid_url(self, fetcher):
        """Test URL validation."""
        assert fetcher._is_valid_url("https://example.com/rss") is True
        assert fetcher._is_valid_url("http://example.com/feed") is True
        assert fetcher._is_valid_url("not-a-url") is False
        assert fetcher._is_valid_url("") is False

    def test_extract_domain(self, fetcher):
        """Test domain extraction from URL."""
        assert fetcher._extract_domain("https://example.com/rss") == "example.com"
        assert fetcher._extract_domain("http://news.ycombinator.com/rss") == "news.ycombinator.com"
