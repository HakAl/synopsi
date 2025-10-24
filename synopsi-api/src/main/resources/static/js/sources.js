checkAuth();

let sources = [];

async function loadSources() {
    try {
        sources = await api.getAllSources();
        renderSources();
    } catch (error) {
        console.error('Error loading sources:', error);
        alert('Error loading sources: ' + (error.message || 'Unknown error'));
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
        item.innerHTML = `
            <div class="source-info">
                <h4>${source.name}</h4>
                <p>${source.url || source.website}</p>
            </div>
            <button class="btn btn-danger" onclick="deleteSource(${source.id})">Remove</button>
        `;
        container.appendChild(item);
    });
}

async function deleteSource(id) {
    if (confirm('Remove this source?')) {
        try {
            await api.deleteSource(id);
            sources = sources.filter(s => s.id !== id);
            renderSources();
        } catch (error) {
            console.error('Error deleting source:', error);
            alert('Error deleting source: ' + (error.message || 'Unknown error'));
        }
    }
}

document.getElementById('addSourceForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const name = document.getElementById('sourceName').value;
    const url = document.getElementById('sourceUrl').value;

    try {
        // API expects sourceData object with specific fields
        const sourceData = {
            name: name,
            url: url,
            type: 'RSS', // Default type
            isActive: true
        };

        const newSource = await api.createSource(sourceData);
        sources.push(newSource);
        renderSources();
        e.target.reset();
        alert('Source added successfully!');
    } catch (error) {
        console.error('Error adding source:', error);
        alert('Error adding source: ' + (error.message || 'Unknown error'));
    }
});

document.addEventListener('DOMContentLoaded', loadSources);