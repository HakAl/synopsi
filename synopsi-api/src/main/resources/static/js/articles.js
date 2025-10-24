checkAuth();

async function loadArticle() {
    const params = new URLSearchParams(window.location.search);
    const articleId = params.get('id');

    if (!articleId) {
        window.location.href = 'dashboard.html';
        return;
    }

    try {
        // Note: You'll need to add a getArticleById method to api.js
        // For now, this uses getSimilarArticles as a workaround to demonstrate the pattern
        // You should add this endpoint: GET /api/v1/articles/{id}

        // Temporary mock until article endpoint is added
        const article = {
            id: articleId,
            title: 'Article Title',
            source: 'Source Name',
            publishedAt: new Date().toISOString(),
            content: 'Full article content would be displayed here. This requires an Article endpoint to be implemented in the backend.',
            url: 'https://example.com/original-article'
        };

        renderArticle(article);

        // Record reading interaction
        await recordReading(articleId);

    } catch (error) {
        console.error('Error loading article:', error);
        alert('Error loading article: ' + (error.message || 'Unknown error'));
        window.location.href = 'dashboard.html';
    }
}

async function recordReading(articleId) {
    try {
        // Track time spent on article (simple implementation)
        const startTime = Date.now();

        window.addEventListener('beforeunload', async () => {
            const timeSpent = Math.floor((Date.now() - startTime) / 1000);
            if (timeSpent > 3) { // Only record if user spent more than 3 seconds
                await api.recordReadingInteraction(articleId, timeSpent);
            }
        });
    } catch (error) {
        console.error('Error recording interaction:', error);
    }
}

function renderArticle(article) {
    const container = document.getElementById('articleContent');
    const date = new Date(article.publishedAt).toLocaleDateString();

    container.innerHTML = `
        <h1>${article.title}</h1>
        <div class="article-meta">
            ${article.source} · ${date} ·
            <a href="${article.url}" target="_blank" rel="noopener">View original</a>
        </div>
        <div class="article-content">
            ${article.content || article.summary}
        </div>
    `;
}

document.addEventListener('DOMContentLoaded', loadArticle);