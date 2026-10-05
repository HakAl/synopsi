import feedparser
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

    @staticmethod
    def _parsed(monkeypatch, **fields):
        """Stub feedparser.parse so the test runs offline with a fixed result."""
        result = feedparser.FeedParserDict(fields)
        monkeypatch.setattr(
            "ingestion.fetchers.rss_fetcher.feedparser.parse",
            lambda url, agent=None: result,
        )
        return result

    def test_nonexistent_feed(self, fetcher, monkeypatch):
        """A 404 page is HTML: feedparser flags it bozo with no entries. That is
        a failed fetch and must raise, not look like an empty feed."""
        self._parsed(
            monkeypatch,
            bozo=1,
            bozo_exception=Exception("syntax error"),
            entries=[],
            status=404,
        )

        with pytest.raises(ValueError, match="HTTP status 404"):
            fetcher.fetch_feed("https://example.com/nonexistent-feed.xml")

    def test_unparseable_body_without_status_raises(self, fetcher, monkeypatch):
        """A local or non-HTTP source has no status; a bozo empty parse still raises."""
        self._parsed(monkeypatch, bozo=1, bozo_exception=Exception("not well-formed"), entries=[])

        with pytest.raises(ValueError, match="could not be parsed"):
            fetcher.fetch_feed("https://example.com/garbage.xml")

    def test_http_error_without_parse_error_raises(self, fetcher, monkeypatch):
        """An HTTP error with an empty body is also a failed fetch."""
        self._parsed(monkeypatch, bozo=0, entries=[], status=500)

        with pytest.raises(ValueError, match="HTTP status 500"):
            fetcher.fetch_feed("https://example.com/feed.xml")

    def test_http_error_with_parseable_body_raises(self, fetcher, monkeypatch):
        """An HTTP error page that happens to parse into entries is still a failure."""
        self._parsed(
            monkeypatch,
            bozo=0,
            entries=[{"title": "Forbidden", "link": "https://example.com/403"}],
            status=403,
        )

        with pytest.raises(ValueError, match="HTTP status 403"):
            fetcher.fetch_feed("https://example.com/feed.xml")

    def test_valid_empty_feed_returns_no_articles(self, fetcher, monkeypatch):
        """A well-formed feed with no items is an empty feed, not a failure."""
        self._parsed(monkeypatch, bozo=0, entries=[], status=200, feed={"title": "Empty"})

        assert fetcher.fetch_feed("https://example.com/empty.xml") == []

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
