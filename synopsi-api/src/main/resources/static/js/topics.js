checkAuth();

let topics = [];
let selectedTopics = new Set();

async function loadTopics() {
    topics = await api.getAllTopics();
    renderTopics();
}

function renderTopics() {
    const container = document.getElementById('topicGrid');
    container.innerHTML = '';

    topics.forEach(topic => {
        const card = document.createElement('div');
        card.className = 'topic-card';
        if (selectedTopics.has(topic.id)) {
            card.classList.add('selected');
        }
        card.textContent = topic.name;
        card.addEventListener('click', () => toggleTopic(topic.id, card));
        container.appendChild(card);
    });
}

function toggleTopic(id, element) {
    if (selectedTopics.has(id)) {
        selectedTopics.delete(id);
        element.classList.remove('selected');
    } else {
        selectedTopics.add(id);
        element.classList.add('selected');
    }

    // API call to save preferences would go here
    console.log('Selected topics:', Array.from(selectedTopics));
}

document.addEventListener('DOMContentLoaded', loadTopics);