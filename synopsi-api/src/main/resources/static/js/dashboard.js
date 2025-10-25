checkAuth();

let currentTopicFilter = 'all';
let summaries = [];

async function loadDashboard() {
    try {
        await loadTopics();
        await loadFeed();
    } catch (error) {
        console.error('Error loading dashboard:', error);
//        todo
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
    }
}

async function loadFeed() {
    try {
        // getPersonalizedFeed returns paginated data
        const feedData = await api.getPersonalizedFeed(0, 20);

        // Handle both array and paginated response
        summaries = feedData.content || feedData;

        renderFeed();
    } catch (error) {
        console.error('Error loading feed:', error);
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
            s.topics && s.topics.some(t => t.toLowerCase() === currentTopicFilter.toLowerCase())
        );
    }

    if (filtered.length === 0) {
        container.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    container.style.display = 'flex';
    emptyState.style.display = 'none';
    container.innerHTML = '';

    filtered.forEach(summary => {
        const card = createSummaryCard(summary);
        container.appendChild(card);
    });
}

function createSummaryCard(summary) {
    const card = document.createElement('div');
    card.className = 'summary-card';

    const date = new Date(summary.publishedAt).toLocaleDateString();

    card.innerHTML = `
        <div class="summary-header">
            <div>
                <div class="summary-title">${summary.title}</div>
                <div class="summary-meta">${summary.source} · ${date}</div>
            </div>
        </div>
        <div class="summary-preview">${summary.preview || summary.summary?.substring(0, 150) + '...'}</div>
        <div class="summary-full" id="summary-${summary.id}">
            <p>${summary.summary}</p>
            <div class="summary-actions">
                <a href="article.html?id=${summary.id}" target="_blank">Read full article →</a>
            </div>
        </div>
    `;

    card.addEventListener('click', (e) => {
        if (e.target.tagName !== 'A') {
            toggleSummary(summary.id);
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

document.addEventListener('DOMContentLoaded', loadDashboard);