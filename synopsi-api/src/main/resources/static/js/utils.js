function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const options = { year: 'numeric', month: 'short', day: 'numeric' };
    return date.toLocaleDateString('en-US', options);
}

function showLoading(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const spinner = document.createElement('div');
    spinner.className = 'loading-spinner';
    spinner.id = `loading-${containerId}`;
    container.innerHTML = '';
    container.appendChild(spinner);
}

function hideLoading(containerId) {
    const spinner = document.getElementById(`loading-${containerId}`);
    if (spinner) {
        spinner.remove();
    }
}