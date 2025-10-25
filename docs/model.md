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

### Topics

----------------
Topic - This is the main entity. It's a hierarchical taxonomy (like a category tree):

ArticleTopic - This is a join table entity. 
It connects Articles to Topics with additional metadata:

articleId + topicId (the relationship)
Potentially a confidence score or relevance weight
Who/what assigned it (user vs worker)

Topic = the taxonomy/categories themselves (predefined list)
ArticleTopic = the many-to-many relationship (which articles belong to which topics)
Article = the content being categorized

### Data Flow

-----------------

- Content ingestion (Feed → Article)
- Content organization (Article → Topic)
- Personalization (User → UserPreference, ReadingHistory, Feedback)
- Recommendation engine training data
