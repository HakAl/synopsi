
## Complete Flow
```
┌─────────────────────────────────────────────────────────────┐
│ INGESTION WORKER (CronJob: every 30 min)                    │
├─────────────────────────────────────────────────────────────┤
│ 1. Fetch RSS/HTML                                           │
│ 2. Clean & extract text                                     │
│ 3. POST /api/v1/articles                                    │
│    ↓                                                         │
│    ArticleService.createArticle()                           │
│    ├─ Save Article (status=NEW)                            │
│    └─ Auto-create SummaryJob (status=QUEUED) ← TRIGGERED   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ SUMMARIZATION WORKER (CronJob: every 10 min)                │
├─────────────────────────────────────────────────────────────┤
│ 1. GET /api/v1/summaries/jobs/queued                           │
│ 2. For each job:                                            │
│    a. GET /api/v1/articles/{articleId}                      │
│    b. Generate summary with T5                              │
│    c. POST /api/v1/summaries/callback/complete                 │
│       ↓                                                      │
│       SummaryService.handleWorkerCallback()                 │
│       ├─ Save Summary                                       │
│       ├─ Mark Job as COMPLETED                              │
│       └─ Update Article.status = SUMMARIZED                 │
└─────────────────────────────────────────────────────────────┘
```

## Benefits of This Architecture

- **Separation of concerns:** Ingestion knows nothing about summarization  
- **Independent scaling:** Can run ingestion every 30 min, summarization every 10 min  
- **Failure isolation:** Ingestion failures don't affect summarization  
- **Queue-based processing:** SummaryJob table acts as a job queue  
- **Retry logic:** Already built into `SummaryService` (max 3 attempts)  

## Python Worker Structure
```
synopsi-worker/
├── ingestion/
│   ├── main.py                    # Entry point for ingestion
│   ├── fetchers/
│   │   ├── rss_fetcher.py
│   │   └── web_scraper.py
│   └── api_client.py              # POST /api/v1/articles
│
├── summarization/
│   ├── main.py                    # Entry point for summarization
│   ├── summarizer.py              # T5 model wrapper
│   ├── preprocessor.py            # Text cleaning
│   └── api_client.py              # GET jobs, POST callback
│
├── requirements-ingestion.txt     # feedparser, beautifulsoup4, requests
├── requirements-summarization.txt # torch, transformers, requests
├── Dockerfile.ingestion
└── Dockerfile.summarization