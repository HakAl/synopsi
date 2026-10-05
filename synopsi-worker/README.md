# Synopsi Worker

Python NLP workers for feed ingestion and article summarization.

## Testing

### Quick Manual Test

Run a quick integration test of both fetchers:

```bash
cd synopsi-worker
python test_fetchers.py
```

This will test:
- RSSFetcher with real RSS feeds (Hacker News, NY Times)
- WebScraper with real web pages (Wikipedia)
- Format consistency between both fetchers

### Unit Tests with Pytest

Install test dependencies:

```bash
pip install -r requirements-test.txt
```

Run all tests:

```bash
pytest tests/
```

Run with coverage:

```bash
pytest tests/ --cov=ingestion --cov=summarization
```

Run specific test file:

```bash
pytest tests/test_rss_fetcher.py
pytest tests/test_web_scraper.py
pytest tests/test_ingestion_worker.py
```

Run tests verbosely:

```bash
pytest tests/ -v
```

## Running Workers

### Ingestion Worker

```bash
cd synopsi-worker
python -m ingestion.main
```

Requires `.env` file with:
- `API_BASE_URL` - Spring Boot API URL
- `API_USERNAME` - API username
- `API_PASSWORD` - API password
- `RSS_FEED_URLS` - Comma-separated feed URLs (RSS or direct web pages)

The API creates this account on startup when it is started with the matching
`SYNOPSI_WORKER_USERNAME` and `SYNOPSI_WORKER_PASSWORD` environment variables
(docker-compose and the Kubernetes manifests already wire these). For a bare
`./gradlew bootRun`, export those two variables before starting the API. The
seeded account has the `WORKER` role, which the summarization worker's routes
(the queued-job list and the completion and failure callbacks) require; an
account created through `POST /api/v1/auth/register` is an ordinary `USER`
and is refused there.

Seeding creates a missing account. An existing `USER` account with the
configured username (one seeded by an earlier version, which created it as
`USER`) is promoted to `WORKER` on the next API start only if
`SYNOPSI_WORKER_PASSWORD` verifies against its stored password; that is what
proves it is the worker's account and not an unrelated registration with the
same name. On a mismatch, or if the account is an `ADMIN` or `MODERATOR`, the
API refuses to start and leaves the account untouched: choose another worker
username or reconcile the password through the API. If you rotate the worker
password in the environment, the stored password of an existing `WORKER`
account is not updated: change it through the API (or reset the database) and
update the workers' `API_PASSWORD` to match.

### Summarization Worker

```bash
cd synopsi-worker
python -m summarization.main
```

## Dependencies

### Ingestion
```bash
pip install -r requirements-ingestion.txt
```

### Summarization
```bash
pip install -r requirements-summarization.txt
```

### Testing
```bash
pip install -r requirements-test.txt
```
