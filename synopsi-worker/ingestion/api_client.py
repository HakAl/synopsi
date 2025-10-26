import requests
import logging
from typing import Dict, List, Optional
from datetime import datetime, timedelta
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from urllib.parse import urlparse

logger = logging.getLogger(__name__)


class SynopsiAPIClient:
    """
    Client for interacting with Synopsi API with JWT authentication.
    Handles login, token refresh, and automatic retry on 401.
    """

    def __init__(
        self,
        base_url: str,
        username: str = None,
        password: str = None,
        access_token: str = None,
        timeout: int = 30
    ):
        """
        Initialize API client with JWT auth.

        Args:
            base_url: Base URL of Synopsi API
            username: Username for login (if no access_token provided)
            password: Password for login (if no access_token provided)
            access_token: Pre-existing access token (optional)
            timeout: Request timeout in seconds
        """
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.username = username
        self.password = password

        self.access_token = access_token
        self.refresh_token = None
        self.token_expiry = None

        self.session = self._create_session()

        # Login if credentials provided and no token
        if not self.access_token and self.username and self.password:
            self.login()
        elif self.access_token:
            self._set_auth_header()

        logger.info(f"Initialized API client with base URL: {self.base_url}")

    def _create_session(self) -> requests.Session:
        """Create requests session with retry logic."""
        session = requests.Session()

        retry_strategy = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[500, 502, 503, 504],
            allowed_methods=["POST", "GET"]
        )

        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)

        session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'Synopsi-Worker/1.0'
        })

        return session

    def login(self) -> None:
        """Login to get access token and refresh token."""
        if not self.username or not self.password:
            raise ValueError("Username and password required for login")

        url = f"{self.base_url}/api/v1/auth/login"

        try:
            logger.info(f"Logging in as {self.username}")

            response = self.session.post(
                url,
                json={
                    'usernameOrEmail': self.username,
                    'password': self.password
                },
                timeout=self.timeout
            )

            if response.status_code == 200:
                data = response.json()

                # Your API returns 'token' not 'accessToken'
                self.access_token = data.get('token')

                # Your API doesn't have refresh tokens, so this will be None
                self.refresh_token = None

                # Your API doesn't return expiry, so we'll assume tokens are long-lived
                # If you want to add expiry later, update your LoginResponseDto
                self.token_expiry = None

                if not self.access_token:
                    raise ValueError("No token in login response")

                self._set_auth_header()
                logger.info("Login successful")
            else:
                logger.error(f"Login failed: {response.status_code} - {response.text}")
                raise ValueError(f"Login failed: {response.text}")

        except Exception as e:
            logger.error(f"Login error: {e}")
            raise

    def refresh_access_token(self) -> None:
        """
        Refresh the access token.
        Since your API doesn't have refresh tokens, we'll just re-login.
        """
        logger.info("No refresh token support, re-authenticating with credentials")
        self.login()

    def _set_auth_header(self):
        """Set Authorization header with access token."""
        if self.access_token:
            self.session.headers.update({
                'Authorization': f'Bearer {self.access_token}'
            })

    def _check_token_expiry(self):
        """Check if token is expired or about to expire, refresh if needed."""
        # Since we don't have token expiry info, skip proactive refresh
        # We'll rely on 401 responses to trigger re-login
        pass

    def _make_request(self, method: str, url: str, **kwargs) -> requests.Response:
        """
        Make HTTP request with automatic token refresh on 401.

        Args:
            method: HTTP method (GET, POST, etc.)
            url: Request URL
            **kwargs: Additional arguments for requests

        Returns:
            Response object
        """
        # Check token expiry before making request
        self._check_token_expiry()

        # Make initial request
        response = self.session.request(method, url, **kwargs)

        # If 401 Unauthorized, try refreshing token and retry once
        if response.status_code == 401:
            logger.warning("Received 401, refreshing token and retrying")
            self.refresh_access_token()
            response = self.session.request(method, url, **kwargs)

        return response

    def _extract_domain_from_url(self, url: str) -> str:
        """
        Extract domain from URL for source name.

        Args:
            url: Full URL

        Returns:
            Domain name (e.g., 'example.com')
        """
        parsed = urlparse(url)
        return parsed.netloc

    def _extract_base_url(self, url: str) -> str:
        """
        Extract base URL (scheme + netloc) from full URL.

        Args:
            url: Full URL

        Returns:
            Base URL (e.g., 'https://example.com')
        """
        parsed = urlparse(url)
        return f"{parsed.scheme}://{parsed.netloc}"

    def create_source(self, feed_url: str) -> Optional[Dict]:
        """
        Create a new source from a feed URL.

        Args:
            feed_url: RSS feed URL

        Returns:
            Created source dictionary with id
        """
        url = f"{self.base_url}/api/v1/sources"

        domain = self._extract_domain_from_url(feed_url)
        base_url = self._extract_base_url(feed_url)

        source_data = {
            'name': domain,
            'baseUrl': base_url,
            'isActive': True,
            'sourceType': 'RSS'
        }

        try:
            logger.info(f"Creating source for domain: {domain}")

            response = self._make_request(
                'POST',
                url,
                json=source_data,
                timeout=self.timeout
            )

            if response.status_code == 201:
                created_source = response.json()
                logger.info(f"Successfully created source ID: {created_source.get('id')} - {domain}")
                return created_source
            elif response.status_code == 400:
                logger.error(f"Bad request creating source: {response.text}")
                raise ValueError(f"Invalid source data: {response.text}")
            else:
                logger.error(f"Failed to create source: {response.status_code} - {response.text}")
                response.raise_for_status()

        except requests.Timeout:
            logger.error(f"Timeout creating source for {domain}")
            raise
        except requests.ConnectionError as e:
            logger.error(f"Connection error creating source: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error creating source: {e}", exc_info=True)
            raise

    def create_feed(self, feed_url: str, source_id: int) -> Optional[Dict]:
        """
        Create a new feed from a URL with specified source ID.

        Args:
            feed_url: RSS feed URL
            source_id: ID of the source this feed belongs to

        Returns:
            Created feed dictionary with id
        """
        url = f"{self.base_url}/api/v1/feeds"

        feed_data = {
            'sourceId': source_id,
            'feedUrl': feed_url,
            'feedType': 'RSS',
            'topicId': None,
            'crawlFrequencyMinutes': 60,
            'isActive': True,
            'priority': 10
        }

        try:
            logger.info(f"Creating feed for URL: {feed_url} (sourceId: {source_id})")

            response = self._make_request(
                'POST',
                url,
                json=feed_data,
                timeout=self.timeout
            )

            if response.status_code == 201:
                created_feed = response.json()
                logger.info(f"Successfully created feed ID: {created_feed.get('id')}")
                return created_feed
            elif response.status_code == 400:
                logger.error(f"Bad request creating feed: {response.text}")
                raise ValueError(f"Invalid feed data: {response.text}")
            else:
                logger.error(f"Failed to create feed: {response.status_code} - {response.text}")
                response.raise_for_status()

        except requests.Timeout:
            logger.error(f"Timeout creating feed for {feed_url}")
            raise
        except requests.ConnectionError as e:
            logger.error(f"Connection error creating feed: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error creating feed: {e}", exc_info=True)
            raise

    def get_all_feeds(self) -> List[Dict]:
        """
        Get all feeds from API.

        Returns:
            List of feed dictionaries
        """
        url = f"{self.base_url}/api/v1/feeds/all"

        try:
            response = self._make_request('GET', url, timeout=self.timeout)

            if response.status_code == 200:
                feeds = response.json()
                logger.info(f"Retrieved {len(feeds)} feeds from API")
                return feeds
            else:
                logger.error(f"Failed to get feeds: {response.status_code} - {response.text}")
                return []

        except Exception as e:
            logger.error(f"Error getting feeds: {e}")
            return []

    def create_article(self, article: Dict) -> Optional[Dict]:
        """Create a new article via POST /api/v1/articles."""
        self._validate_article(article)
        url = f"{self.base_url}/api/v1/articles"

        try:
            logger.debug(f"Creating article: {article.get('title', 'Unknown')}")

            response = self._make_request(
                'POST',
                url,
                json=article,
                timeout=self.timeout
            )

            if response.status_code == 201:
                created_article = response.json()
                logger.info(f"Successfully created article ID: {created_article.get('id')}")
                return created_article

            elif 400 <= response.status_code < 500:
                logger.error(f"Client error creating article: {response.status_code} - {response.text}")
                raise ValueError(f"Invalid article data: {response.text}")

            else:
                logger.error(f"Server error creating article: {response.status_code} - {response.text}")
                response.raise_for_status()

        except requests.Timeout:
            logger.error(f"Timeout creating article: {article.get('title')}")
            raise
        except requests.ConnectionError as e:
            logger.error(f"Connection error creating article: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error creating article: {e}", exc_info=True)
            raise

    def get_article(self, article_id: int) -> Optional[Dict]:
        """Get article by ID via GET /api/v1/articles/{id}."""
        url = f"{self.base_url}/api/v1/articles/{article_id}"

        try:
            response = self._make_request('GET', url, timeout=self.timeout)

            if response.status_code == 200:
                return response.json()
            elif response.status_code == 404:
                logger.warning(f"Article not found: {article_id}")
                return None
            else:
                response.raise_for_status()

        except Exception as e:
            logger.error(f"Error fetching article {article_id}: {e}")
            raise

    def health_check(self) -> bool:
        """Check if API is reachable."""
        try:
            url = f"{self.base_url}/api/v1/articles?page=0&size=1"
            response = self._make_request('GET', url, timeout=5)

            is_healthy = response.status_code < 500
            if is_healthy:
                logger.info("API health check passed")
            else:
                logger.warning(f"API health check failed: {response.status_code}")

            return is_healthy

        except Exception as e:
            logger.error(f"API health check failed: {e}")
            return False

    def create_articles_batch(self, articles: List[Dict]) -> Dict[str, List]:
        """Create multiple articles and return success/failure results."""
        results = {
            'successful': [],
            'failed': []
        }

        logger.info(f"Creating {len(articles)} articles in batch")

        for article in articles:
            try:
                created_article = self.create_article(article)
                if created_article:
                    results['successful'].append(created_article)
            except Exception as e:
                logger.warning(f"Failed to create article '{article.get('title', 'Unknown')}': {e}")
                results['failed'].append({
                    'article': article,
                    'error': str(e)
                })

        logger.info(
            f"Batch complete: {len(results['successful'])} successful, "
            f"{len(results['failed'])} failed"
        )

        return results

    def _validate_article(self, article: Dict) -> None:
        """Validate article has required fields."""
        required_fields = ['title', 'originalUrl']
        missing_fields = [field for field in required_fields if not article.get(field)]

        if missing_fields:
            raise ValueError(f"Article missing required fields: {missing_fields}")

        url = article.get('originalUrl', '')
        if not url.startswith(('http://', 'https://')):
            raise ValueError(f"Invalid URL format: {url}")

    def close(self):
        """Close the session and cleanup resources."""
        self.session.close()
        logger.info("API client closed")