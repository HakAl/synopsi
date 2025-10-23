## Core Entity Set

-------------

**1. User**
- Basic authentication and profile
- Foundation for all personalization

**2. Article**
- The central content entity
- Links to Source and Feed

**3. Topic/Category**
- Content organization
- Supports hierarchical structures

**4. UserPreference**
- User's topic interests
- Drives personalization algorithm

**5. ReadingHistory**
- Tracks user engagement
- Critical for ML/recommendations

**6. UserArticleFeedback**
- Explicit user signals (likes/saves/shares)
- Complements implicit signals from ReadingHistory

**7. Source**
- Represents publishers/websites
- High-level source metadata

**8. Feed**
- RSS/Atom feed URLs
- Feed-specific configuration and crawl management

**9. ArticleTopic** (Join Table)
- Many-to-many Article ↔ Topic
- Optional relevanceScore for weighted relationships

## Key Relationships

-------------

```
Source (1) → (*) Feed
Feed (1) → (*) Article
Article (*) ↔ (*) Topic (via ArticleTopic)
User (*) ↔ (*) Topic (via UserPreference)
User → ReadingHistory ← Article
User → UserArticleFeedback ← Article
```


- Content ingestion (Feed → Article)
- Content organization (Article → Topic)
- Personalization (User → UserPreference, ReadingHistory, Feedback)
- Recommendation engine training data
