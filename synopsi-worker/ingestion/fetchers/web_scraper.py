import requests
import logging
from typing import Dict, Optional, List
from datetime import datetime
from bs4 import BeautifulSoup
from urllib.parse import urlparse, urljoin
import re

logger = logging.getLogger(__name__)


class WebScraper:
    """
    Scrapes article content from web pages using requests and BeautifulSoup.

    Can handle two scenarios:
    1. Single article pages - extracts one article
    2. News listing pages - finds article links and scrapes each one
    """

    def __init__(self, user_agent: str = "Synopsi/1.0", timeout: int = 30, max_articles: int = 10):
        """
        Initialize web scraper with custom user agent.

        Args:
            user_agent: User agent string for HTTP requests
            timeout: Request timeout in seconds
            max_articles: Maximum number of articles to scrape from listing pages
        """
        self.user_agent = user_agent
        self.timeout = timeout
        self.max_articles = max_articles
        self.session = self._create_session()

    def _create_session(self) -> requests.Session:
        """Create requests session with headers."""
        session = requests.Session()
        session.headers.update({
            'User-Agent': self.user_agent,
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate',
            'Connection': 'keep-alive',
        })
        return session

    def scrape_page(self, url: str) -> List[Dict]:
        """
        Scrape articles from a web page.

        Auto-detects whether the URL is:
        - A listing page (homepage, blog index) -> extracts article links and scrapes each
        - A single article -> scrapes that article

        Args:
            url: URL of the page to scrape

        Returns:
            List of normalized article dictionaries

        Raises:
            ValueError: If URL is invalid or scraping fails
        """
        if not url or not self._is_valid_url(url):
            raise ValueError(f"Invalid URL: {url}")

        logger.info(f"Scraping page: {url}")

        try:
            # Fetch and parse the page
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()
            soup = BeautifulSoup(response.content, 'html.parser')

            # Check if this looks like a listing page
            if self._is_listing_page(soup, url):
                logger.info(f"Detected listing page: {url}")
                return self._scrape_listing_page(url, soup)
            else:
                logger.info(f"Detected single article: {url}")
                article = self._scrape_single_article(url, soup)
                return [article]

        except requests.RequestException as e:
            logger.error(f"HTTP error scraping {url}: {e}")
            raise ValueError(f"Failed to fetch URL: {e}")
        except Exception as e:
            logger.error(f"Failed to scrape {url}: {e}", exc_info=True)
            raise ValueError(f"Scraping failed: {e}")

    def scrape_article(self, url: str) -> Dict:
        """
        Scrape a single article from a web page.

        Note: For new code, consider using scrape_page() which returns List[Dict]
        to match RSSFetcher's interface.

        Args:
            url: URL of the article to scrape

        Returns:
            Normalized article dictionary with extracted content

        Raises:
            ValueError: If URL is invalid or scraping fails
        """
        if not url or not self._is_valid_url(url):
            raise ValueError(f"Invalid URL: {url}")

        logger.info(f"Scraping article: {url}")

        try:
            # Fetch HTML
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()

            # Parse HTML
            soup = BeautifulSoup(response.content, 'html.parser')

            # Use shared helper
            return self._scrape_single_article(url, soup)

        except requests.RequestException as e:
            logger.error(f"HTTP error scraping {url}: {e}")
            raise ValueError(f"Failed to fetch URL: {e}")
        except Exception as e:
            logger.error(f"Failed to scrape {url}: {e}", exc_info=True)
            raise ValueError(f"Scraping failed: {e}")

    def _scrape_single_article(self, url: str, soup: BeautifulSoup) -> Dict:
        """Extract article data from a BeautifulSoup object."""
        # Extract article components
        title = self._extract_title(soup, url)
        content = self._extract_content(soup)
        author = self._extract_author(soup)
        publication_date = self._extract_publication_date(soup)
        description = self._extract_description(soup)
        language = self._extract_language(soup)
        source = self._extract_domain(url)

        # Build normalized article
        article = {
            'title': title,
            'originalUrl': url,
            'content': content,
            'description': description,
            'author': author,
            'publicationDate': publication_date,
            'source': source,
            'feedTitle': source,  # Use domain as feed title for scraped articles
            'language': language
        }

        logger.info(f"Successfully scraped article: {title}")
        return article

    def _is_listing_page(self, soup: BeautifulSoup, url: str) -> bool:
        """
        Determine if a page is a listing page or a single article.

        Heuristics:
        - Homepage-like URLs (/, /news, /blog)
        - Multiple article links with similar structure
        - Presence of article listing containers
        """
        url_lower = url.lower()

        # Check if URL looks like a listing page
        listing_patterns = [
            r'/$',  # Homepage
            r'/index',
            r'/news/?$',
            r'/blog/?$',
            r'/articles/?$',
            r'/category/',
            r'/section/',
        ]

        for pattern in listing_patterns:
            if re.search(pattern, url_lower):
                return True

        # Count potential article links
        article_links = self._extract_article_links(url, soup)
        if len(article_links) >= 3:  # If we find 3+ article links, likely a listing page
            return True

        return False

    def _scrape_listing_page(self, base_url: str, soup: BeautifulSoup) -> List[Dict]:
        """
        Scrape multiple articles from a listing page.

        Args:
            base_url: URL of the listing page
            soup: BeautifulSoup object of the listing page

        Returns:
            List of article dictionaries
        """
        # Extract article links
        article_urls = self._extract_article_links(base_url, soup)

        if not article_urls:
            logger.warning(f"No article links found on listing page: {base_url}")
            return []

        logger.info(f"Found {len(article_urls)} article links on {base_url}")

        # Limit number of articles
        article_urls = article_urls[:self.max_articles]
        logger.info(f"Scraping first {len(article_urls)} articles")

        # Scrape each article
        articles = []
        for article_url in article_urls:
            try:
                article = self.scrape_article(article_url)
                articles.append(article)
                logger.info(f"Scraped article {len(articles)}/{len(article_urls)}: {article['title']}")
            except Exception as e:
                logger.error(f"Failed to scrape article {article_url}: {e}")
                continue

        logger.info(f"Successfully scraped {len(articles)} articles from {base_url}")
        return articles

    def _extract_article_links(self, base_url: str, soup: BeautifulSoup) -> List[str]:
        """
        Extract article links from a listing page.

        Args:
            base_url: URL of the listing page (for resolving relative links)
            soup: BeautifulSoup object

        Returns:
            List of article URLs
        """
        article_urls = []
        seen_urls = set()

        # Find all links
        links = soup.find_all('a', href=True)

        for link in links:
            href = link['href']

            # Convert relative URLs to absolute
            absolute_url = urljoin(base_url, href)

            # Skip if already seen
            if absolute_url in seen_urls:
                continue

            # Check if this looks like an article link
            if self._is_article_link(absolute_url, base_url):
                article_urls.append(absolute_url)
                seen_urls.add(absolute_url)

        return article_urls

    def _is_article_link(self, url: str, base_url: str) -> bool:
        """
        Determine if a link is likely an article.

        Heuristics:
        - Same domain as base URL
        - Not a navigation/utility page
        - Contains article indicators in path
        """
        # Must be same domain
        base_domain = urlparse(base_url).netloc
        link_domain = urlparse(url).netloc
        if link_domain != base_domain:
            return False

        url_lower = url.lower()

        # Exclude common non-article pages
        exclude_patterns = [
            r'/tag/',
            r'/category/',
            r'/author/',
            r'/search',
            r'/login',
            r'/register',
            r'/about',
            r'/contact',
            r'/privacy',
            r'/terms',
            r'/advertise',
            r'/subscribe',
            r'#',  # Anchor links
            r'\.(pdf|jpg|jpeg|png|gif|mp3|mp4|zip)$',  # Files
        ]

        for pattern in exclude_patterns:
            if re.search(pattern, url_lower):
                return False

        # Include if URL has article indicators
        include_patterns = [
            r'/\d{4}/\d{2}/',  # Date in path (2024/11/)
            r'/article/',
            r'/post/',
            r'/story/',
            r'/news/',
            r'/blog/',
            r'-\d+$',  # Ends with ID (article-12345)
        ]

        for pattern in include_patterns:
            if re.search(pattern, url_lower):
                return True

        # If path has at least 2 segments and doesn't end in /, likely an article
        path = urlparse(url).path
        if path and path != '/' and not path.endswith('/'):
            segments = [s for s in path.split('/') if s]
            if len(segments) >= 2:
                return True

        return False

    def _extract_title(self, soup: BeautifulSoup, url: str) -> str:
        """
        Extract article title from HTML.
        Tries multiple strategies: meta tags, h1, title tag.
        """
        # Try Open Graph title
        og_title = soup.find('meta', property='og:title')
        if og_title and og_title.get('content'):
            return og_title['content'].strip()

        # Try Twitter title
        twitter_title = soup.find('meta', attrs={'name': 'twitter:title'})
        if twitter_title and twitter_title.get('content'):
            return twitter_title['content'].strip()

        # Try article:title
        article_title = soup.find('meta', property='article:title')
        if article_title and article_title.get('content'):
            return article_title['content'].strip()

        # Try h1 tag
        h1 = soup.find('h1')
        if h1:
            return h1.get_text().strip()

        # Try title tag (fallback)
        if soup.title:
            title = soup.title.get_text().strip()
            # Remove common suffixes like " | Site Name"
            title = re.split(r'[|-]', title)[0].strip()
            return title

        # Last resort: use domain name
        return self._extract_domain(url)

    def _extract_content(self, soup: BeautifulSoup) -> str:
        """
        Extract main article content from HTML.
        Uses heuristics to find the main text content.
        """
        # Remove unwanted elements
        for element in soup(['script', 'style', 'nav', 'header', 'footer', 'aside', 'iframe']):
            element.decompose()

        # Try common article containers (in order of specificity)
        article_selectors = [
            'article',
            '[role="main"]',
            '.article-content',
            '.post-content',
            '.entry-content',
            '.content-body',
            '.article-body',
            'main',
            '.main-content',
        ]

        content = None
        for selector in article_selectors:
            container = soup.select_one(selector)
            if container:
                content = self._extract_text_from_element(container)
                if content and len(content) > 200:  # Reasonable article length
                    break

        # Fallback: find all paragraphs
        if not content or len(content) < 200:
            paragraphs = soup.find_all('p')
            content = '\n\n'.join([p.get_text().strip() for p in paragraphs if p.get_text().strip()])

        return content.strip() if content else ''

    def _extract_text_from_element(self, element) -> str:
        """Extract clean text from a BeautifulSoup element."""
        # Get all paragraph text
        paragraphs = element.find_all('p')
        if paragraphs:
            text = '\n\n'.join([p.get_text().strip() for p in paragraphs if p.get_text().strip()])
            return text

        # Fallback: get all text
        return element.get_text(separator='\n', strip=True)

    def _extract_author(self, soup: BeautifulSoup) -> Optional[str]:
        """
        Extract article author from HTML meta tags.
        """
        # Try various meta tag formats
        author_selectors = [
            ('meta', {'name': 'author'}),
            ('meta', {'property': 'article:author'}),
            ('meta', {'name': 'article:author'}),
            ('meta', {'property': 'og:article:author'}),
            ('meta', {'name': 'twitter:creator'}),
            ('meta', {'name': 'parsely-author'}),
            ('meta', {'name': 'sailthru.author'}),
        ]

        for tag, attrs in author_selectors:
            element = soup.find(tag, attrs=attrs)
            if element and element.get('content'):
                return element['content'].strip()

        # Try schema.org markup
        schema_author = soup.find(attrs={'itemprop': 'author'})
        if schema_author:
            name = schema_author.find(attrs={'itemprop': 'name'})
            if name:
                return name.get_text().strip()
            return schema_author.get_text().strip()

        # Try common class names
        author_classes = ['.author', '.byline', '.by-author', '.article-author']
        for class_name in author_classes:
            element = soup.select_one(class_name)
            if element:
                text = element.get_text().strip()
                # Clean common prefixes
                text = re.sub(r'^(by|author:?)\s+', '', text, flags=re.IGNORECASE)
                return text

        return None

    def _extract_publication_date(self, soup: BeautifulSoup) -> Optional[str]:
        """
        Extract publication date from HTML meta tags.
        Returns ISO 8601 format string or None.
        """
        # Try various meta tag formats
        date_selectors = [
            ('meta', {'property': 'article:published_time'}),
            ('meta', {'name': 'article:published_time'}),
            ('meta', {'property': 'og:published_time'}),
            ('meta', {'name': 'publishdate'}),
            ('meta', {'name': 'publish-date'}),
            ('meta', {'name': 'date'}),
            ('meta', {'name': 'DC.date.issued'}),
            ('meta', {'name': 'parsely-pub-date'}),
            ('meta', {'name': 'sailthru.date'}),
        ]

        for tag, attrs in date_selectors:
            element = soup.find(tag, attrs=attrs)
            if element and element.get('content'):
                date_str = element['content'].strip()
                return self._normalize_date(date_str)

        # Try schema.org markup
        schema_date = soup.find(attrs={'itemprop': 'datePublished'})
        if schema_date and schema_date.get('content'):
            return self._normalize_date(schema_date['content'])

        # Try time tag
        time_tag = soup.find('time')
        if time_tag:
            datetime_attr = time_tag.get('datetime')
            if datetime_attr:
                return self._normalize_date(datetime_attr)

        return None

    def _normalize_date(self, date_str: str) -> str:
        """
        Attempt to parse and normalize date string to ISO 8601.
        If parsing fails, return the original string.
        """
        try:
            # Try common formats
            formats = [
                '%Y-%m-%dT%H:%M:%S%z',
                '%Y-%m-%dT%H:%M:%S',
                '%Y-%m-%d %H:%M:%S',
                '%Y-%m-%d',
                '%Y/%m/%d',
            ]

            for fmt in formats:
                try:
                    dt = datetime.strptime(date_str[:19], fmt[:19])
                    return dt.isoformat()
                except ValueError:
                    continue

            # Return original if parsing fails
            return date_str

        except Exception:
            return date_str

    def _extract_description(self, soup: BeautifulSoup) -> str:
        """
        Extract article description/summary from meta tags.
        """
        # Try Open Graph description
        og_desc = soup.find('meta', property='og:description')
        if og_desc and og_desc.get('content'):
            return og_desc['content'].strip()

        # Try Twitter description
        twitter_desc = soup.find('meta', attrs={'name': 'twitter:description'})
        if twitter_desc and twitter_desc.get('content'):
            return twitter_desc['content'].strip()

        # Try standard meta description
        meta_desc = soup.find('meta', attrs={'name': 'description'})
        if meta_desc and meta_desc.get('content'):
            return meta_desc['content'].strip()

        return ''

    def _extract_language(self, soup: BeautifulSoup) -> Optional[str]:
        """Extract language from HTML lang attribute or meta tags."""
        # Try html lang attribute
        html = soup.find('html')
        if html and html.get('lang'):
            return html['lang'].strip()

        # Try meta tag
        meta_lang = soup.find('meta', attrs={'http-equiv': 'content-language'})
        if meta_lang and meta_lang.get('content'):
            return meta_lang['content'].strip()

        return None

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

    def close(self):
        """Close the requests session."""
        self.session.close()
