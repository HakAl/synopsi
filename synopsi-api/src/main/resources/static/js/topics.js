checkAuth();

let topics = [];
let selectedTopics = new Set();

async function loadTopics() {
    try {
        hideError('topicError');
        topics = await api.getAllTopics();
        await loadUserPreferences();
        renderTopics();
    } catch (error) {
        console.error('Error loading topics:', error);
        const errorMsg = extractErrorMessage(error, 'Unable to load topics. Please try again.');
        showError('topicError', errorMsg);
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
        throw error;
    }
}

function renderTopics() {
    const container = document.getElementById('topicGrid');
    container.innerHTML = '';

    if (topics.length === 0) {
        const emptyTemplate = document.getElementById('emptyTopicsTemplate');
        const emptyClone = emptyTemplate.content.cloneNode(true);
        container.appendChild(emptyClone);
        return;
    }

    topics.forEach(topic => {
        const template = document.getElementById('topicCardTemplate');
        const clone = template.content.cloneNode(true);
        const card = clone.querySelector('.topic-card');

        if (selectedTopics.has(topic.id)) {
            card.classList.add('selected');
        }

        card.textContent = topic.name;
        card.addEventListener('click', () => toggleTopic(topic.id, card));

        container.appendChild(clone);
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

    try {
        hideError('topicError');
        await api.updateUserPreference({
            topicId: topicId,
            isActive: !wasSelected
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

        const errorMsg = extractErrorMessage(error, 'Unable to update preference. Please try again.');
        showError('topicError', errorMsg);
    }
}

function showError(containerId, message) {
    const errorContainer = document.getElementById(containerId);
    const errorText = document.getElementById(containerId + 'Text');
    errorText.textContent = message;
    errorContainer.style.display = 'block';
}

function hideError(containerId) {
    const errorContainer = document.getElementById(containerId);
    errorContainer.style.display = 'none';
}

function extractErrorMessage(error, defaultMessage) {
    // Check for general error message (user-friendly messages only)
    if (error.data?.error && typeof error.data.error === 'string') {
        // Only use if it's a user-friendly message, not technical jargon
        const errorMsg = error.data.error;
        if (!errorMsg.toLowerCase().includes('validation') &&
            !errorMsg.toLowerCase().includes('failed')) {
            return errorMsg;
        }
    }

    // Always fall back to default message for validation errors or technical messages
    return defaultMessage;
}

document.addEventListener('DOMContentLoaded', loadTopics);