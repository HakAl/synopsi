checkAuth();

let sources = [];
let editingSourceId = null;

async function loadSources() {
    try {
        hideError('listError');
        sources = await api.getAllSources();
        renderSources();
    } catch (error) {
        console.error('Error loading sources:', error);
        const errorMsg = error.data?.message || error.message || 'Failed to load sources. Please try again.';
        showError('listError', errorMsg);
    }
}

function renderSources() {
    const container = document.getElementById('sourceList');
    container.innerHTML = '';

    if (sources.length === 0) {
        const emptyTemplate = document.getElementById('emptyStateTemplate');
        const emptyClone = emptyTemplate.content.cloneNode(true);
        container.appendChild(emptyClone);
        return;
    }

    sources.forEach(source => {
        const template = document.getElementById('sourceItemTemplate');
        const clone = template.content.cloneNode(true);

        const nameEl = clone.querySelector('.source-name');
        const urlEl = clone.querySelector('.source-url');
        const dateEl = clone.querySelector('.source-date');
        const typeBadge = clone.querySelector('.source-type-badge');
        const statusBadge = clone.querySelector('.source-status-badge');
        const editBtn = clone.querySelector('.edit-btn');
        const deleteBtn = clone.querySelector('.delete-btn');

        nameEl.textContent = source.name;
        urlEl.textContent = source.baseUrl;
        urlEl.href = source.baseUrl;
        dateEl.textContent = `Added: ${formatDate(source.createdAt)}`;

        if (source.sourceType) {
            typeBadge.textContent = source.sourceType;
        } else {
            typeBadge.remove();
        }

        if (source.isActive) {
            statusBadge.remove();
        } else {
            statusBadge.textContent = 'Inactive';
        }

        editBtn.addEventListener('click', () => editSource(source.id));
        deleteBtn.addEventListener('click', () => deleteSource(source.id));

        container.appendChild(clone);
    });
}

function editSource(id) {
    const source = sources.find(s => s.id === id);
    if (!source) return;

    editingSourceId = id;
    hideError('formError');

    document.getElementById('formTitle').textContent = 'Edit Source';
    document.getElementById('sourceId').value = source.id;
    document.getElementById('sourceName').value = source.name;
    document.getElementById('sourceUrl').value = source.baseUrl;
    document.getElementById('sourceType').value = source.sourceType || '';
    document.getElementById('isActive').value = source.isActive.toString();
    document.getElementById('submitBtn').textContent = 'Update Source';
    document.getElementById('cancelBtn').style.display = 'inline-block';

    document.getElementById('addSourceForm').scrollIntoView({ behavior: 'smooth' });
}

function cancelEdit() {
    editingSourceId = null;
    hideError('formError');

    document.getElementById('formTitle').textContent = 'Add New Source';
    document.getElementById('addSourceForm').reset();
    document.getElementById('sourceId').value = '';
    document.getElementById('submitBtn').textContent = 'Add Source';
    document.getElementById('cancelBtn').style.display = 'none';
}

async function deleteSource(id) {
    if (!confirm('Remove this source? This action cannot be undone.')) {
        return;
    }

    try {
        hideError('listError');
        await api.deleteSource(id);
        sources = sources.filter(s => s.id !== id);
        renderSources();

        if (editingSourceId === id) {
            cancelEdit();
        }
    } catch (error) {
        console.error('Error deleting source:', error);
        const errorMsg = error.data?.message || error.message || 'Failed to delete source. Please try again.';
        showError('listError', errorMsg);
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

document.getElementById('addSourceForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideError('formError');

    const name = document.getElementById('sourceName').value;
    const baseUrl = document.getElementById('sourceUrl').value;
    const sourceType = document.getElementById('sourceType').value || null;
    const isActive = document.getElementById('isActive').value === 'true';

    try {
        const sourceData = {
            name: name,
            baseUrl: baseUrl,
            sourceType: sourceType,
            isActive: isActive
        };

        if (editingSourceId) {
            const updatedSource = await api.updateSource(editingSourceId, sourceData);
            sources = sources.map(s => s.id === editingSourceId ? updatedSource : s);
            cancelEdit();
        } else {
            const newSource = await api.createSource(sourceData);
            sources.push(newSource);
            e.target.reset();
        }

        renderSources();
    } catch (error) {
        console.error('Error saving source:', error);
        const errorMsg = error.data?.message || error.message || 'Failed to save source. Please try again.';
        showError('formError', errorMsg);
    }
});

document.getElementById('cancelBtn').addEventListener('click', cancelEdit);

document.addEventListener('DOMContentLoaded', loadSources);