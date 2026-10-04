checkAuth();

let currentTopicFilter = 'all';
let summaries = [];

async function loadDashboard() {
    try {
        await loadTopics();
        await loadFeed();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('Failed to load dashboard. Please try refreshing the page.');
    }
}

async function loadTopics() {
    try {
        const topics = await api.getAllTopics({ active: true });
        const topicSelect = document.getElementById('topicSelect');

        // Add topics as options
        topics.forEach(topic => {
            const option = document.createElement('option');
            option.value = topic.slug;
            option.textContent = topic.name;
            topicSelect.appendChild(option);
        });

        // Add change event listener
        topicSelect.addEventListener('change', (e) => {
            filterByTopic(e.target.value);
        });
    } catch (error) {
        console.error('Error loading topics:', error);
        showError('Failed to load topics.');
    }
}

async function loadFeed() {
    try {
        showLoading('feedContainer');

        // getPersonalizedFeed returns paginated data
        const feedData = await api.getPersonalizedFeed(0, 20);

        // Handle both array and paginated response
        summaries = feedData.content || feedData;

        hideLoading('feedContainer');
        renderFeed();
    } catch (error) {
        console.error('Error loading feed:', error);
        hideLoading('feedContainer');
        showError('Failed to load your personalized feed.');
        document.getElementById('emptyState').style.display = 'block';
        document.getElementById('feedContainer').style.display = 'none';
    }
}

function renderFeed() {
    const container = document.getElementById('feedContainer');
    const emptyState = document.getElementById('emptyState');

    let filtered = summaries;
    if (currentTopicFilter !== 'all') {
        filtered = summaries.filter(s =>
            s.topicNames && s.topicNames.some(t => t.toLowerCase() === currentTopicFilter.toLowerCase())
        );
    }

    if (filtered.length === 0) {
        container.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    container.style.display = 'flex';
    emptyState.style.display = 'none';
    container.textContent = '';

    filtered.forEach(summary => {
        const card = createSummaryCard(summary);
        container.appendChild(card);
    });
}

function createSummaryCard(summary) {
    const card = document.createElement('div');
    card.className = 'summary-card';

    const date = new Date(summary.publicationDate).toLocaleDateString();

    // Create header
    const header = document.createElement('div');
    header.className = 'summary-header';

    const headerContent = document.createElement('div');

    const title = document.createElement('div');
    title.className = 'summary-title';
    title.textContent = summary.title;

    const meta = document.createElement('div');
    meta.className = 'summary-meta';
    meta.textContent = `${summary.sourceName} · ${date}`;

    headerContent.appendChild(title);
    headerContent.appendChild(meta);
    header.appendChild(headerContent);

    // Create preview
    const preview = document.createElement('div');
    preview.className = 'summary-preview';
    preview.textContent = summary.summary ? summary.summary.substring(0, 150) + '...' : (summary.description || 'No summary yet.');

    // Create full summary section
    const fullSummary = document.createElement('div');
    fullSummary.className = 'summary-full';
    fullSummary.id = `summary-${summary.articleId}`;

    const summaryText = document.createElement('p');
    summaryText.textContent = summary.summary;

    const actions = document.createElement('div');
    actions.className = 'summary-actions';

    const link = document.createElement('a');
    link.href = `article.html?id=${summary.articleId}`;
    link.target = '_blank';
    link.textContent = 'Read full article →';

    actions.appendChild(link);
    fullSummary.appendChild(summaryText);
    fullSummary.appendChild(actions);

    // Assemble card
    card.appendChild(header);
    card.appendChild(preview);
    card.appendChild(fullSummary);

    card.addEventListener('click', (e) => {
        if (e.target.tagName !== 'A') {
            toggleSummary(summary.articleId);
        }
    });

    return card;
}

function toggleSummary(id) {
    const summaryDiv = document.getElementById(`summary-${id}`);
    summaryDiv.classList.toggle('expanded');
}

function filterByTopic(topic) {
    currentTopicFilter = topic;
    renderFeed();
}

function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';

        // Auto-hide after 5 seconds
        setTimeout(() => {
            errorDiv.style.display = 'none';
        }, 5000);
    }
}

document.addEventListener('DOMContentLoaded', loadDashboard);