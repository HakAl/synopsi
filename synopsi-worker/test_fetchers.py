"""
Quick test script to verify RSSFetcher and WebScraper work correctly.
Run from synopsi-worker directory: python test_fetchers.py
"""

import sys
import logging
from ingestion.fetchers.rss_fetcher import RSSFetcher
from ingestion.fetchers.web_scraper import WebScraper

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def validate_article(article: dict, source: str) -> bool:
    """Validate that article has all required fields."""
    required_fields = [
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

    logger.info(f"\n{'='*60}")
    logger.info(f"Validating article from {source}")
    logger.info(f"{'='*60}")

    missing_fields = []
    for field in required_fields:
        if field not in article:
            missing_fields.append(field)
            logger.error(f"❌ Missing required field: {field}")
        else:
            value = article[field]
            value_preview = str(value)[:50] if value else "None"
            logger.info(f"✓ {field}: {value_preview}")

    if missing_fields:
        logger.error(f"❌ Validation failed: missing {missing_fields}")
        return False

    logger.info(f"✓ Article validation passed!")
    return True


def test_rss_fetcher():
    """Test RSSFetcher with a real RSS feed."""
    logger.info("\n" + "="*60)
    logger.info("TESTING RSS FETCHER")
    logger.info("="*60)

    # Use a reliable test RSS feed
    test_feeds = [
        "https://news.ycombinator.com/rss",  # Hacker News
        "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",  # NY Times World
    ]

    fetcher = RSSFetcher(user_agent="Synopsi-Test/1.0")

    for feed_url in test_feeds:
        try:
            logger.info(f"\nTesting RSS feed: {feed_url}")
            articles = fetcher.fetch_feed(feed_url)

            if not articles:
                logger.warning(f"⚠ No articles returned from {feed_url}")
                continue

            logger.info(f"✓ Fetched {len(articles)} articles")

            # Validate first article
            first_article = articles[0]
            if validate_article(first_article, "RSSFetcher"):
                logger.info(f"✓ RSS Fetcher test PASSED for {feed_url}")
            else:
                logger.error(f"❌ RSS Fetcher test FAILED for {feed_url}")
                return False

        except Exception as e:
            logger.error(f"❌ Error testing RSS feed {feed_url}: {e}", exc_info=True)
            return False

    return True


def test_web_scraper():
    """Test WebScraper with real web pages."""
    logger.info("\n" + "="*60)
    logger.info("TESTING WEB SCRAPER")
    logger.info("="*60)

    # Test both single article and listing page
    test_cases = [
        ("https://en.wikipedia.org/wiki/Python_(programming_language)", "single article"),
        ("https://npr.org", "listing page"),
    ]

    scraper = WebScraper(user_agent="Synopsi-Test/1.0", max_articles=3)

    for url, page_type in test_cases:
        try:
            logger.info(f"\nTesting {page_type}: {url}")
            articles = scraper.scrape_page(url)

            if not articles:
                logger.error(f"❌ No articles returned for {url}")
                scraper.close()
                return False

            logger.info(f"✓ Scraped {len(articles)} article(s)")

            # Validate first article
            if validate_article(articles[0], f"WebScraper ({page_type})"):
                logger.info(f"✓ Web Scraper test PASSED for {url}")
            else:
                logger.error(f"❌ Web Scraper test FAILED for {url}")
                scraper.close()
                return False

        except Exception as e:
            logger.error(f"❌ Error scraping {url}: {e}", exc_info=True)
            scraper.close()
            return False

    scraper.close()
    return True


def test_format_consistency():
    """Test that both fetchers return the same format."""
    logger.info("\n" + "="*60)
    logger.info("TESTING FORMAT CONSISTENCY")
    logger.info("="*60)

    # Fetch one article from each source
    rss_fetcher = RSSFetcher()
    web_scraper = WebScraper()

    try:
        # Get RSS article
        rss_articles = rss_fetcher.fetch_feed("https://news.ycombinator.com/rss")
        rss_article = rss_articles[0] if rss_articles else None

        # Get web article
        web_articles = web_scraper.scrape_page("https://en.wikipedia.org/wiki/Python_(programming_language)")
        web_article = web_articles[0] if web_articles else None

        if not rss_article or not web_article:
            logger.error("❌ Failed to fetch test articles")
            return False

        # Compare keys
        rss_keys = set(rss_article.keys())
        web_keys = set(web_article.keys())

        # Allow feedId to be missing (added later)
        rss_keys.discard('feedId')
        web_keys.discard('feedId')

        if rss_keys == web_keys:
            logger.info(f"✓ Both fetchers return identical keys: {sorted(rss_keys)}")
            logger.info(f"✓ Format consistency test PASSED")
            return True
        else:
            logger.error(f"❌ Key mismatch!")
            logger.error(f"RSS only: {rss_keys - web_keys}")
            logger.error(f"Web only: {web_keys - rss_keys}")
            return False

    except Exception as e:
        logger.error(f"❌ Error testing format consistency: {e}", exc_info=True)
        return False
    finally:
        web_scraper.close()


def main():
    """Run all tests."""
    logger.info("\n" + "="*60)
    logger.info("SYNOPSI FETCHER TEST SUITE")
    logger.info("="*60)

    results = {
        "RSS Fetcher": test_rss_fetcher(),
        "Web Scraper": test_web_scraper(),
        "Format Consistency": test_format_consistency(),
    }

    # Print summary
    logger.info("\n" + "="*60)
    logger.info("TEST SUMMARY")
    logger.info("="*60)

    all_passed = True
    for test_name, passed in results.items():
        status = "✓ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")
        if not passed:
            all_passed = False

    logger.info("="*60)

    if all_passed:
        logger.info("✓ ALL TESTS PASSED!")
        sys.exit(0)
    else:
        logger.error("❌ SOME TESTS FAILED")
        sys.exit(1)


if __name__ == '__main__':
    main()
