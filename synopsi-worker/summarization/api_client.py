import requests
import logging
from typing import Dict, List, Optional
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

logger = logging.getLogger(__name__)


class SummaryAPIClient:
    """
    Client for interacting with Synopsi API summary endpoints.
    Handles JWT authentication and automatic retry on 401.
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

        self.session = self._create_session()

        # Login if credentials provided and no token
        if not self.access_token and self.username and self.password:
            self.login()
        elif self.access_token:
            self._set_auth_header()

        logger.info(f"Initialized Summary API client with base URL: {self.base_url}")

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
            'User-Agent': 'Synopsi-Summarization/1.0'
        })

        return session

    def login(self) -> None:
        """Login to get access token."""
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
                self.access_token = data.get('token')

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

    def _set_auth_header(self):
        """Set Authorization header with access token."""
        if self.access_token:
            self.session.headers.update({
                'Authorization': f'Bearer {self.access_token}'
            })

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
        response = self.session.request(method, url, **kwargs)

        # If 401 Unauthorized, try re-login and retry once
        if response.status_code == 401:
            logger.warning("Received 401, re-authenticating and retrying")
            self.login()
            response = self.session.request(method, url, **kwargs)

        return response

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

    def get_queued_jobs(self) -> List[Dict]:
        """
        Get queued summary jobs from API.

        Returns:
            List of SummaryJob dictionaries
        """
        url = f"{self.base_url}/api/v1/summaries/jobs/queued"

        try:
            logger.info("Fetching queued summary jobs")
            response = self._make_request('GET', url, timeout=self.timeout)

            if response.status_code == 200:
                jobs = response.json()
                logger.info(f"Retrieved {len(jobs)} queued jobs")
                return jobs
            else:
                logger.error(f"Failed to get queued jobs: {response.status_code} - {response.text}")
                return []

        except Exception as e:
            logger.error(f"Error fetching queued jobs: {e}", exc_info=True)
            return []

    def get_article(self, article_id: int) -> Optional[Dict]:
        """
        Get article by ID to retrieve content for summarization.

        Args:
            article_id: ID of the article

        Returns:
            Article dictionary with content, or None if not found
        """
        url = f"{self.base_url}/api/v1/articles/{article_id}"

        try:
            logger.debug(f"Fetching article {article_id}")
            response = self._make_request('GET', url, timeout=self.timeout)

            if response.status_code == 200:
                article = response.json()
                logger.debug(f"Retrieved article: {article.get('title', 'Unknown')}")
                return article
            elif response.status_code == 404:
                logger.warning(f"Article not found: {article_id}")
                return None
            else:
                logger.error(f"Failed to get article {article_id}: {response.status_code}")
                return None

        except Exception as e:
            logger.error(f"Error fetching article {article_id}: {e}", exc_info=True)
            return None

    def complete_job(
        self,
        job_id: int,
        summary_text: str,
        model_version: str,
        token_count: int = None
    ) -> bool:
        """
        Report successful job completion to API.

        Args:
            job_id: ID of the summary job
            summary_text: Generated summary text
            model_version: Model identifier (e.g., "distilbart-cnn-12-6")
            token_count: Optional token count for analytics

        Returns:
            True if callback succeeded, False otherwise
        """
        url = f"{self.base_url}/api/v1/summaries/callback/complete"

        params = {
            'jobId': job_id,
            'summaryText': summary_text,
            'modelVersion': model_version
        }
        if token_count is not None:
            params['tokenCount'] = token_count

        try:
            logger.info(f"Completing job {job_id}")
            response = self._make_request(
                'POST',
                url,
                params=params,
                timeout=self.timeout
            )

            if response.status_code in (200, 204):
                logger.info(f"Job {job_id} completed successfully")
                return True
            else:
                logger.error(f"Failed to complete job {job_id}: {response.status_code} - {response.text}")
                return False

        except Exception as e:
            logger.error(f"Error completing job {job_id}: {e}", exc_info=True)
            return False

    def fail_job(self, job_id: int, error_message: str) -> bool:
        """
        Report job failure to API.

        Args:
            job_id: ID of the summary job
            error_message: Description of the error

        Returns:
            True if callback succeeded, False otherwise
        """
        url = f"{self.base_url}/api/v1/summaries/callback/failure"

        params = {
            'jobId': job_id,
            'errorMessage': error_message[:500]  # Truncate long error messages
        }

        try:
            logger.info(f"Reporting failure for job {job_id}: {error_message[:100]}")
            response = self._make_request(
                'POST',
                url,
                params=params,
                timeout=self.timeout
            )

            if response.status_code == 200:
                logger.info(f"Job {job_id} failure reported")
                return True
            else:
                logger.error(f"Failed to report job failure: {response.status_code} - {response.text}")
                return False

        except Exception as e:
            logger.error(f"Error reporting job failure {job_id}: {e}", exc_info=True)
            return False

    def close(self):
        """Close the session and cleanup resources."""
        self.session.close()
        logger.info("Summary API client closed")
