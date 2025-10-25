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
        showError(error.message || 'The article could not be found.');
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
    // Show article, hide error
    document.getElementById('articleContent').style.display = 'block';
    document.getElementById('errorMessage').style.display = 'none';

    // Title
    document.getElementById('articleTitle').textContent = article.title;

    // Meta information
    const date = article.publicationDate
        ? new Date(article.publicationDate).toLocaleDateString()
        : 'Unknown date';

    const sourceInfo = article.sourceName
        ? article.sourceName
        : (article.feedTitle || 'Unknown source');

    const readTime = article.readTimeMinutes
        ? ` · ${article.readTimeMinutes} min read`
        : '';

    const originalLink = article.originalUrl
        ? ` · <a href="${article.originalUrl}" target="_blank" rel="noopener">View original</a>`
        : '';

    document.getElementById('articleMeta').innerHTML =
        `${sourceInfo} · ${date}${readTime}${originalLink}`;

    // Image
    const imageElement = document.getElementById('articleImage');
    if (article.imageUrl) {
        imageElement.src = article.imageUrl;
        imageElement.alt = article.title;
        imageElement.style.display = 'block';
    } else {
        imageElement.style.display = 'none';
    }

    // Description
    const descElement = document.getElementById('articleDescription');
    if (article.description) {
        descElement.textContent = article.description;
        descElement.style.display = 'block';
    } else {
        descElement.style.display = 'none';
    }

    // Content
    const content = article.content || article.summary || 'No content available.';
    document.getElementById('articleBody').textContent = content;

    // Author
    const authorElement = document.getElementById('articleAuthor');
    if (article.author) {
        authorElement.textContent = `By ${article.author}`;
        authorElement.style.display = 'block';
    } else {
        authorElement.style.display = 'none';
    }
}

function showError(message) {
    // Hide article, show error
    document.getElementById('articleContent').style.display = 'none';
    document.getElementById('errorMessage').style.display = 'block';
    document.getElementById('errorText').textContent = message;
}

document.addEventListener('DOMContentLoaded', loadArticle);