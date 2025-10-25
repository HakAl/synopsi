checkAuth();

let sources = [];
let editingSourceId = null;

async function loadSources() {
    try {
        sources = await api.getAllSources();
        renderSources();
    } catch (error) {
        console.error('Error loading sources:', error);
//        todo
    }
}

function renderSources() {
    const container = document.getElementById('sourceList');

    if (sources.length === 0) {
        container.innerHTML = '<p style="color: var(--text-light)">No sources added yet</p>';
        return;
    }

    container.innerHTML = '';
    sources.forEach(source => {
        const item = document.createElement('div');
        item.className = 'source-item';

        const formattedDate = formatDate(source.createdAt);
        const sourceTypeBadge = source.sourceType
            ? `<span class="source-badge">${source.sourceType}</span>`
            : '';
        const statusBadge = !source.isActive
            ? `<span class="source-badge inactive">Inactive</span>`
            : '';

        item.innerHTML = `
            <div class="source-info">
                <h4>${escapeHtml(source.name)}</h4>
                <a href="${escapeHtml(source.baseUrl)}" target="_blank" class="source-url" rel="noopener noreferrer">
                    ${escapeHtml(source.baseUrl)}
                </a>
                <div class="source-meta">
                    <span>Added: ${formattedDate}</span>
                    ${sourceTypeBadge}
                    ${statusBadge}
                </div>
            </div>
            <div class="source-actions">
                <button class="btn btn-secondary" onclick="editSource(${source.id})">Edit</button>
                <button class="btn btn-danger" onclick="deleteSource(${source.id})">Remove</button>
            </div>
        `;
        container.appendChild(item);
    });
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const options = { year: 'numeric', month: 'short', day: 'numeric' };
    return date.toLocaleDateString('en-US', options);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function editSource(id) {
    const source = sources.find(s => s.id === id);
    if (!source) return;

    editingSourceId = id;

    document.getElementById('formTitle').textContent = 'Edit Source';
    document.getElementById('sourceId').value = source.id;
    document.getElementById('sourceName').value = source.name;
    document.getElementById('sourceUrl').value = source.baseUrl;
    document.getElementById('sourceType').value = source.sourceType || '';
    document.getElementById('isActive').value = source.isActive.toString();
    document.getElementById('submitBtn').textContent = 'Update Source';
    document.getElementById('cancelBtn').style.display = 'inline-block';

    // Scroll to form
    document.getElementById('addSourceForm').scrollIntoView({ behavior: 'smooth' });
}

function cancelEdit() {
    editingSourceId = null;

    document.getElementById('formTitle').textContent = 'Add New Source';
    document.getElementById('addSourceForm').reset();
    document.getElementById('sourceId').value = '';
    document.getElementById('submitBtn').textContent = 'Add Source';
    document.getElementById('cancelBtn').style.display = 'none';
}

async function deleteSource(id) {
    if (confirm('Remove this source? This action cannot be undone.')) {
        try {
            await api.deleteSource(id);
            sources = sources.filter(s => s.id !== id);
            renderSources();

            // If we were editing this source, cancel the edit
            if (editingSourceId === id) {
                cancelEdit();
            }
        } catch (error) {
            console.error('Error deleting source:', error);
//            todo
        }
    }
}

document.getElementById('addSourceForm').addEventListener('submit', async (e) => {
    e.preventDefault();

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
            // Update existing source
            const updatedSource = await api.updateSource(editingSourceId, sourceData);
            sources = sources.map(s => s.id === editingSourceId ? updatedSource : s);
//            todo
            cancelEdit();
        } else {
            // Create new source
            const newSource = await api.createSource(sourceData);
            sources.push(newSource);
            e.target.reset();
        }

        renderSources();
    } catch (error) {
        console.error('Error saving source:', error);
        const errorMsg = error.data?.message || error.message || 'Unknown error';
//        todo
    }
});

document.addEventListener('DOMContentLoaded', loadSources);