### Worker Gets Feeds to Crawl

```GET http://localhost:8080/api/v1/feeds/needs-crawl```

### Worker Reports Success

```POST http://localhost:8080/api/v1/feeds/123/crawl/success```

### Worker Reports Failure

```
POST http://localhost:8080/api/v1/feeds/123/crawl/failure
Content-Type: application/json

{
"errorMessage": "Connection timeout after 30 seconds"
}
Create a Feed
bashPOST http://localhost:8080/api/v1/feeds
Content-Type: application/json

{
"sourceId": 1,
"feedUrl": "https://example.com/rss",
"feedType": "RSS",
"title": "Example Feed",
"crawlFrequencyMinutes": 60,
"priority": 7,
"isActive": true
}
```

### Get Filtered Feeds

```GET http://localhost:8080/api/v1/feeds?sourceId=1&isActive=true&minPriori```