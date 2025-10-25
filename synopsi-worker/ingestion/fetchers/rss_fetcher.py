import feedparser
import logging
from typing import List, Dict, Optional
from datetime import datetime
from urllib.parse import urlparse

logger = logging.getLogger(__name__)


class RSSFetcher:
    """
    Fetches articles from RSS/Atom feeds using feedparser.
    Handles feed parsing, error handling, and data normalization.
    """

    def __init__(self, user_agent: str = "Synopsi/1.0"):
        """
        Initialize RSS fetcher with custom user agent.

        Args:
            user_agent: User agent string for HTTP requests
        """
        self.user_agent = user_agent

    def fetch_feed(self, feed_url: str) -> List[Dict]:
        """
        Fetch and parse a single RSS/Atom feed.

        Args:
            feed_url: URL of the RSS/Atom feed

        Returns:
            List of article dictionaries with normalized fields

        Raises:
            ValueError: If feed URL is invalid or feed parsing fails
        """
        if not feed_url or not self._is_valid_url(feed_url):
            raise ValueError(f"Invalid feed URL: {feed_url}")

        logger.info(f"Fetching RSS feed: {feed_url}")

        try:
            # Parse feed with custom user agent
            feed = feedparser.parse(feed_url, agent=self.user_agent)

            # Check for parsing errors
            if feed.bozo:
                logger.warning(f"Feed parsing warning for {feed_url}: {feed.bozo_exception}")

            # Check if feed has entries
            if not hasattr(feed, 'entries') or len(feed.entries) == 0:
                logger.warning(f"No entries found in feed: {feed_url}")
                return []

            # Extract feed metadata
            feed_title = self._get_feed_title(feed)
            feed_source = self._extract_domain(feed_url)

            # Normalize each entry
            articles = []
            for entry in feed.entries:
                try:
                    article = self._normalize_entry(entry, feed_title, feed_source)
                    articles.append(article)
                except Exception as e:
                    logger.error(f"Failed to normalize entry: {e}", exc_info=True)
                    continue

            logger.info(f"Successfully fetched {len(articles)} articles from {feed_url}")
            return articles

        except Exception as e:
            logger.error(f"Failed to fetch feed {feed_url}: {e}", exc_info=True)
            raise ValueError(f"Feed fetch failed: {e}")

    def fetch_multiple_feeds(self, feed_urls: List[str]) -> Dict[str, List[Dict]]:
        """
        Fetch multiple RSS feeds and return results grouped by feed URL.

        Args:
            feed_urls: List of RSS feed URLs

        Returns:
            Dictionary mapping feed URL to list of articles
        """
        results = {}

        for feed_url in feed_urls:
            try:
                articles = self.fetch_feed(feed_url)
                results[feed_url] = articles
            except Exception as e:
                logger.error(f"Skipping feed {feed_url} due to error: {e}")
                results[feed_url] = []

        total_articles = sum(len(articles) for articles in results.values())
        logger.info(f"Fetched {total_articles} total articles from {len(feed_urls)} feeds")

        return results

    def _normalize_entry(self, entry, feed_title: str, feed_source: str) -> Dict:
        """
        Normalize a feed entry to a standard article dictionary format.

        Args:
            entry: feedparser entry object
            feed_title: Title of the feed
            feed_source: Domain name of the feed

        Returns:
            Normalized article dictionary
        """
        # Extract title (required)
        title = entry.get('title', '').strip()
        if not title:
            raise ValueError("Entry missing required field: title")

        # Extract link/URL (required)
        link = entry.get('link', '').strip()
        if not link:
            raise ValueError("Entry missing required field: link")

        # Extract content (try multiple fields)
        content = self._extract_content(entry)

        # Extract description/summary
        description = entry.get('summary', '').strip() or entry.get('description', '').strip()

        # Extract author
        author = self._extract_author(entry)

        # Extract publication date
        publication_date = self._extract_date(entry)

        # Extract language (if available)
        language = entry.get('language') or entry.get('content_language')

        # Build normalized article
        article = {
            'title': title,
            'originalUrl': link,
            'content': content,
            'description': description,
            'author': author,
            'publicationDate': publication_date,
            'source': feed_source,
            'feedTitle': feed_title,
            'language': language
        }

        return article

    def _extract_content(self, entry) -> str:
        """
        Extract full content from entry, trying multiple fields.
        Priority: content > summary > description
        """
        # Try content field first (most detailed)
        if hasattr(entry, 'content') and len(entry.content) > 0:
            return entry.content[0].get('value', '').strip()

        # Try summary_detail
        if hasattr(entry, 'summary_detail'):
            return entry.summary_detail.get('value', '').strip()

        # Fall back to summary
        if hasattr(entry, 'summary'):
            return entry.summary.strip()

        # Fall back to description
        if hasattr(entry, 'description'):
            return entry.description.strip()

        return ''

    def _extract_author(self, entry) -> Optional[str]:
        """Extract author from entry, handling various formats."""
        # Try author field
        if hasattr(entry, 'author'):
            return entry.author.strip()

        # Try author_detail
        if hasattr(entry, 'author_detail') and 'name' in entry.author_detail:
            return entry.author_detail['name'].strip()

        # Try dc:creator (Dublin Core)
        if hasattr(entry, 'dc_creator'):
            return entry.dc_creator.strip()

        return None

    def _extract_date(self, entry) -> Optional[str]:
        """
        Extract publication date and convert to ISO 8601 format.
        Returns ISO format string or None if date not found.
        """
        # Try published_parsed first
        if hasattr(entry, 'published_parsed') and entry.published_parsed:
            return self._struct_time_to_iso(entry.published_parsed)

        # Try updated_parsed
        if hasattr(entry, 'updated_parsed') and entry.updated_parsed:
            return self._struct_time_to_iso(entry.updated_parsed)

        # Try published string
        if hasattr(entry, 'published'):
            return entry.published

        # Try updated string
        if hasattr(entry, 'updated'):
            return entry.updated

        return None

    def _struct_time_to_iso(self, struct_time) -> str:
        """Convert time.struct_time to ISO 8601 string."""
        dt = datetime(*struct_time[:6])
        return dt.isoformat()

    def _get_feed_title(self, feed) -> str:
        """Extract feed title with fallback."""
        if hasattr(feed, 'feed') and hasattr(feed.feed, 'title'):
            return feed.feed.title.strip()
        return "Unknown Feed"

    def _extract_domain(self, url: str) -> str:
        """Extract domain name from URL."""
        parsed = urlparse(url)
        return parsed.netloc or "unknown"

    def _is_valid_url(self, url: str) -> bool:
        """Validate URL format."""
        try:
            result = urlparse(url)
            return all([result.scheme, result.netloc])
        except Exception:
            return False