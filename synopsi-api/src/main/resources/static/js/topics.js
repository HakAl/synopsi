checkAuth();

let topics = [];
let selectedTopics = new Set();

async function loadTopics() {
    try {
        topics = await api.getAllTopics();
        await loadUserPreferences();
        renderTopics();
    } catch (error) {
        console.error('Error loading topics:', error);
        alert('Error loading topics: ' + (error.message || 'Unknown error'));
    }
}

async function loadUserPreferences() {
    try {
        const preferences = await api.getUserPreferences();
        preferences.forEach(pref => {
            selectedTopics.add(pref.topicId);
        });
    } catch (error) {
        console.error('Error loading preferences:', error);
    }
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

async function toggleTopic(topicId, element) {
    const wasSelected = selectedTopics.has(topicId);

    if (wasSelected) {
        selectedTopics.delete(topicId);
        element.classList.remove('selected');
    } else {
        selectedTopics.add(topicId);
        element.classList.add('selected');
    }

    // Save preference to API
    try {
        await api.updateUserPreference({
            topicId: topicId,
            preferenceLevel: wasSelected ? 0 : 5 // 0 = deselect, 5 = moderate interest
        });
    } catch (error) {
        console.error('Error updating preference:', error);
        // Revert UI on error
        if (wasSelected) {
            selectedTopics.add(topicId);
            element.classList.add('selected');
        } else {
            selectedTopics.delete(topicId);
            element.classList.remove('selected');
        }
    }
}

document.addEventListener('DOMContentLoaded', loadTopics);