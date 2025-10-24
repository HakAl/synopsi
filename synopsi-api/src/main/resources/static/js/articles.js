checkAuth();

async function loadArticle() {
    const params = new URLSearchParams(window.location.search);
    const articleId = params.get('id');

    if (!articleId) {
        window.location.href = 'dashboard.html';
        return;
    }

    // Mock article data - will be replaced with API call
    const article = {
        id: articleId,
        title: 'Article Title',
        source: 'Source Name',
        publishedAt: '2025-10-24T10:30:00Z',
        content: 'Full article content would be displayed here...',
        url: 'https://example.com/original-article'
    };

    renderArticle(article);
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
            ${article.content}
        </div>
    `;
}

document.addEventListener('DOMContentLoaded', loadArticle);