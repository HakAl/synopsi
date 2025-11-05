# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Synopsi is a personalized news and learning summarizer that turns RSS feeds into concise, relevant briefings. The system consists of:

- **synopsi-api**: Spring Boot 3 REST API (Java 21) that serves the dashboard and manages data
- **Frontend**: Plain HTML/CSS/JavaScript dashboard (no framework) served from `synopsi-api/src/main/resources/static/`
- **synopsi-worker**: Python NLP workers for feed ingestion and article summarization
- **Database**: H2 (in-memory for dev), designed for PostgreSQL in production

## Common Commands

### Java/Spring Boot API

```bash
# Run the Spring Boot API (from project root)
./gradlew bootRun

# Run tests
./gradlew test

# Build the project
./gradlew build

# Run a specific test
./gradlew test --tests "com.study.synopsi.service.SummaryServiceTest"

# Clean build
./gradlew clean build
```

### Python Workers

```bash
# Run ingestion worker (requires .env with API_BASE_URL, API_USERNAME, API_PASSWORD, RSS_FEED_URLS)
cd synopsi-worker
python -m ingestion.main

# Run summarization worker
python -m summarization.main

# Install ingestion dependencies
pip install -r requirements-ingestion.txt

# Install summarization dependencies
pip install -r requirements-summarization.txt
```

### Frontend

When the API is running, the frontend is available at: http://localhost:8080/

The frontend is plain HTML/CSS/JavaScript served from `synopsi-api/src/main/resources/static/`

### H2 Database Console

When the API is running, access the H2 console at: http://localhost:8080/h2-console

Connection details are in `synopsi-api/src/main/resources/application.properties`

### API Documentation

Swagger UI is available at: http://localhost:8080/swagger-ui.html

## Architecture

### Worker Flow

The system uses a **two-worker architecture** with job-based processing:

1. **Ingestion Worker** (runs every 30 min):
   - Fetches RSS feeds
   - Creates Source and Feed entities
   - POSTs articles to `/api/v1/articles`
   - Article creation **auto-triggers** a SummaryJob (status=QUEUED)

2. **Summarization Worker** (runs every 10 min):
   - GETs queued jobs from `/api/v1/summaries/jobs/queued`
   - Downloads article content
   - Generates summaries using T5/DistilBART model
   - POSTs results to `/api/v1/summaries/callback/complete`
   - SummaryService handles the callback and updates job status

**Key Insight**: Workers are **decoupled**. Ingestion doesn't call summarization directly. The `SummaryJob` table acts as a job queue between them.

### Core Domain Model

- **Article**: Central content entity (title, content, url, publishedAt, status)
- **Feed**: RSS/Atom feed URLs with crawl configuration
- **Source**: Publisher/website metadata (one Source has many Feeds)
- **Topic**: Hierarchical taxonomy for content organization
- **ArticleTopic**: Join table (Article ↔ Topic) with optional relevanceScore
- **Summary**: Generated summaries (linked to Article, supports types: BRIEF/DETAILED, lengths: SHORT/MEDIUM/LONG)
- **SummaryJob**: Tracks summarization requests (status: QUEUED/IN_PROGRESS/COMPLETED/FAILED, includes retry logic with max 3 attempts)
- **User**: Basic authentication and profile
- **UserPreference**: User's topic interests
- **ReadingHistory**: Tracks engagement (completion %, read time)
- **UserArticleFeedback**: Explicit signals (likes, saves, shares)

### Key Relationships

```
Source (1) → (*) Feed
Feed (1) → (*) Article
Article (*) ↔ (*) Topic (via ArticleTopic)
Article (1) → (*) Summary
User (*) ↔ (*) Topic (via UserPreference)
User → ReadingHistory ← Article
User → UserArticleFeedback ← Article
```

### Package Structure (synopsi-api)

```
com.study.synopsi/
├── controller/         # REST endpoints
├── service/           # Business logic
├── repository/        # JPA repositories
├── model/            # JPA entities
├── dto/              # Request/Response DTOs
├── mapper/           # MapStruct mappers (entity ↔ DTO)
├── specification/    # Dynamic query builders
├── exception/        # Custom exceptions
└── config/           # Configuration classes
```

### API Endpoints (Key)

**Articles**
- `POST /api/v1/articles` - Create article (triggers auto-summary job)
- `GET /api/v1/articles?status=NEW&feedId=1` - Filtered article list with pagination

**Feeds** (Worker-facing)
- `GET /api/v1/feeds/needs-crawl` - Get feeds due for crawling
- `POST /api/v1/feeds/{id}/crawl/success` - Report crawl success
- `POST /api/v1/feeds/{id}/crawl/failure` - Report crawl failure

**Summaries** (Worker-facing)
- `GET /api/v1/summaries/jobs/queued` - Get jobs for summarization worker
- `POST /api/v1/summaries/callback/complete?jobId=1&summaryText=...` - Worker success callback
- `POST /api/v1/summaries/callback/failure?jobId=1&errorMessage=...` - Worker failure callback

**Summaries** (User-facing)
- `POST /api/v1/summaries/request?articleId=1&summaryType=BRIEF` - Request new summary
- `GET /api/v1/summaries/article/1?userId=1&summaryType=BRIEF` - Get summary with fallback

**Personalization**
- `GET /api/v1/personalization/feed?userId=1` - Personalized article feed

### Frontend (Static HTML/JS)

The frontend is a plain HTML/CSS/JavaScript application located in `synopsi-api/src/main/resources/static/`. It uses **no frameworks** - just vanilla JS with a modular structure.

**Pages** (`static/*.html`):
- `index.html` - Login page
- `register.html` - User registration
- `forgot-password.html` / `reset-password.html` - Password reset flow
- `dashboard.html` - Personalized feed (main page after login)
- `sources.html` - Manage RSS feeds
- `topics.html` - Topic management and preferences
- `article.html` - Individual article view
- `settings.html` - User settings

**JavaScript Modules** (`static/js/`):
- `api.js` - HTTP client with automatic JWT token injection, token management (localStorage)
- `auth.js` - Authentication utilities and route guards
- `dashboard.js` - Personalized feed rendering
- `articles.js` - Article list and filtering
- `sources.js` - Feed/source management
- `topics.js` - Topic browsing and preference management
- `settings.js` - User settings page
- `login.js` / `register.js` / `password.js` - Auth flows
- `utils.js` - Shared utilities

**Key Frontend Patterns**:
- JWT stored in `localStorage` with key `synopsi_jwt_token`
- User info stored in `localStorage` with key `synopsi_user`
- All API requests go through `httpClient()` in `api.js` which auto-injects the token
- API base URL is hardcoded to `http://localhost:8080` in `api.js`
- Auth redirects handled by checking token validity on page load

**Access**: When the Spring Boot API is running, the frontend is available at http://localhost:8080/

### Configuration

**Spring Configuration** (`application.properties`):
- Personalization scoring weights (topic-preference: 0.40, reading-history: 0.30, positive-feedback: 0.20, recency: 0.10)
- Cache settings (Caffeine, 10m TTL, 1000 max size)
- JWT secret and expiration (24 hours)
- H2 console enabled

**Python Workers** (`.env` required):
- `API_BASE_URL` - Spring Boot API URL (default: http://localhost:8080)
- `API_USERNAME`, `API_PASSWORD` - API authentication
- `RSS_FEED_URLS` - Comma-separated RSS feed URLs

### Key Technologies

- **Backend**: Spring Boot 3.3.5, Spring Data JPA, Spring Security, Spring Cache (Caffeine)
- **Frontend**: Vanilla JavaScript (ES6+), HTML5, CSS3 (no frameworks)
- **Database**: H2 (dev), PostgreSQL (prod target)
- **Auth**: JWT (jjwt library) with localStorage-based token management
- **Mapping**: MapStruct 1.5.5
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Python**: Python 3.11+
- **NLP**: PyTorch 2.9.0, Transformers 4.57.1 (T5/DistilBART)
- **Parsing**: feedparser 6.0.12, beautifulsoup4 4.14.2

### Testing

Tests use JUnit 5 with Spring Boot Test. Key test files:
- `SummaryServiceTest` - Summary job creation and callbacks
- `FeedServiceTest` - Feed management
- `UserServiceUnitTest` / `UserServiceIntegrationTest` - User operations
- `ArticleControllerTest` - Article REST endpoints

Run with: `./gradlew test --tests "ClassName"`

### Important Implementation Notes

1. **Auto-Summary Creation**: When creating an Article via `ArticleService.createArticle()`, a default SummaryJob is automatically created. This is the trigger mechanism for the summarization worker.

2. **Job-Based Processing**: The SummaryJob entity includes retry logic (max 3 attempts) and failure tracking. Workers should always use the callback endpoints to update job status.

3. **Personalization Algorithm**: The PersonalizationService uses weighted scoring combining topic preferences, reading history, explicit feedback, and recency decay. Weights are configurable in `application.properties`.

4. **Feed Crawl Management**: Feeds have `lastCrawlAt`, `nextCrawlAt`, and `crawlFrequencyMinutes` fields. Workers should use `GET /api/v1/feeds/needs-crawl` to get feeds ready for processing.

5. **Security**: The API uses JWT-based authentication. Python workers must authenticate and include the JWT token in subsequent requests.

6. **MapStruct**: Entity-DTO mapping uses MapStruct annotation processors. Generated implementations are in `build/generated/sources/annotationProcessor/`.
