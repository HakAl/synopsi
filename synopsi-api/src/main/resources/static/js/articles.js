checkAuth();

async function loadArticle() {
    const params = new URLSearchParams(window.location.search);
    const articleId = params.get('id');

    if (!articleId) {
        window.location.href = 'dashboard.html';
        return;
    }

    try {
        const article = await api.getArticle(articleId);
        renderArticle(article);

        // Record reading interaction
        await recordReading(articleId);

    } catch (error) {
        console.error('Error loading article:', error);

        // Show user-friendly error message
        const container = document.getElementById('articleContent');
        container.innerHTML = `
            <div class="error-message">
                <h2>Unable to load article</h2>
                <p>${error.message || 'The article could not be found.'}</p>
                <a href="dashboard.html" class="back-link">Return to Feed</a>
            </div>
        `;
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
    const date = article.publicationDate
        ? new Date(article.publicationDate).toLocaleDateString()
        : 'Unknown date';

    // Use article properties from ArticleResponseDto
    const sourceInfo = article.sourceName
        ? `${article.sourceName}`
        : (article.feedTitle || 'Unknown source');

    const content = article.content || article.summary || 'No content available.';
    const readTime = article.readTimeMinutes
        ? `${article.readTimeMinutes} min read`
        : '';

    container.innerHTML = `
        <h1>${article.title}</h1>
        <div class="article-meta">
            ${sourceInfo} · ${date}
            ${readTime ? `· ${readTime}` : ''}
            ${article.originalUrl ? `· <a href="${article.originalUrl}" target="_blank" rel="noopener">View original</a>` : ''}
        </div>
        ${article.imageUrl ? `<img src="${article.imageUrl}" alt="${article.title}" class="article-image">` : ''}
        ${article.description ? `<p class="article-description">${article.description}</p>` : ''}
        <div class="article-content">
            ${content}
        </div>
        ${article.author ? `<p class="article-author">By ${article.author}</p>` : ''}
    `;
}

document.addEventListener('DOMContentLoaded', loadArticle);