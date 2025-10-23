## Testing

```
# Request a default summary
POST http://localhost:8080/api/summaries/request?articleId=1&summaryType=BRIEF&summaryLength=MEDIUM

# Request a user-specific summary
POST http://localhost:8080/api/summaries/request?articleId=1&userId=1&summaryType=DETAILED&summaryLength=LONG

# Check job status
GET http://localhost:8080/api/summaries/jobs/{jobId}

# Get queued jobs
GET http://localhost:8080/api/summaries/jobs/queued

# Get job statistics
GET http://localhost:8080/api/summaries/jobs/statistics

# Check if summary exists
GET http://localhost:8080/api/summaries/exists?articleId=1&summaryType=BRIEF
```

### Retrieval

```
# Get summary for article (with fallback to default)
GET http://localhost:8080/api/summaries/article/1?userId=1&summaryType=BRIEF

# Get default summary only
GET http://localhost:8080/api/summaries/article/1/default?summaryType=BRIEF

# Get all summaries for an article
GET http://localhost:8080/api/summaries/article/1/all
```

### Worker

```
# Worker callback when complete
POST http://localhost:8080/api/summaries/callback/complete?jobId=1&summaryText=...&modelVersion=pytorch-v1.0&tokenCount=150

# Worker failure callback
POST http://localhost:8080/api/summaries/callback/failure?jobId=1&errorMessage=Model timeout
```


### Powershell
```
# First, create an article
# Then request a summary
Invoke-RestMethod -Uri "http://localhost:8080/api/summaries/request?articleId=1" -Method POST

# Check the job was created
Invoke-RestMethod -Uri "http://localhost:8080/api/summaries/jobs/statistics" -Method GET
```