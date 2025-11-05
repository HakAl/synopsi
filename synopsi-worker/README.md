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
