import pytest
from ingestion.fetchers.web_scraper import WebScraper


class TestWebScraper:
    """Test suite for WebScraper."""

    @pytest.fixture
    def scraper(self):
        """Create WebScraper instance for testing."""
        scraper = WebScraper(user_agent="Synopsi-Test/1.0")
        yield scraper
        scraper.close()

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

    def test_scrape_wikipedia_article(self, scraper, required_fields):
        """Test scraping a Wikipedia article."""
        url = "https://en.wikipedia.org/wiki/Python_(programming_language)"
        article = scraper.scrape_article(url)

        # Check all required fields exist
        for field in required_fields:
            assert field in article, f"Article should have '{field}' field"

        # Verify critical fields are not empty
        assert article['title'], "Title should not be empty"
        assert article['originalUrl'] == url, "Original URL should match"
        assert article['content'], "Content should not be empty"
        assert len(article['content']) > 200, "Content should be substantial"

    def test_invalid_url(self, scraper):
        """Test handling of invalid URL."""
        with pytest.raises(ValueError):
            scraper.scrape_article("not-a-valid-url")

    def test_nonexistent_page(self, scraper):
        """Test handling of non-existent page (404)."""
        with pytest.raises(ValueError):
            scraper.scrape_article("https://example.com/nonexistent-page-12345")

    def test_is_valid_url(self, scraper):
        """Test URL validation."""
        assert scraper._is_valid_url("https://example.com/article") is True
        assert scraper._is_valid_url("http://example.com/page") is True
        assert scraper._is_valid_url("not-a-url") is False
        assert scraper._is_valid_url("") is False

    def test_extract_domain(self, scraper):
        """Test domain extraction from URL."""
        assert scraper._extract_domain("https://example.com/article") == "example.com"
        assert scraper._extract_domain("https://en.wikipedia.org/wiki/Python") == "en.wikipedia.org"

    def test_normalize_date(self, scraper):
        """Test date normalization."""
        # ISO 8601 format
        result = scraper._normalize_date("2024-01-15T10:30:00")
        assert "2024-01-15" in result

        # Simple date format
        result = scraper._normalize_date("2024-01-15")
        assert "2024-01-15" in result

        # Invalid format should return original
        result = scraper._normalize_date("invalid-date")
        assert result == "invalid-date"
