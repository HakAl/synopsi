checkAuth();

let sources = [];

async function loadSources() {
    sources = await api.getAllSources();
    renderSources();
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
                <p>${source.url}</p>
            </div>
            <button class="btn btn-danger" onclick="deleteSource(${source.id})">Remove</button>
        `;
        container.appendChild(item);
    });
}

async function deleteSource(id) {
    if (confirm('Remove this source?')) {
        await api.deleteSource(id);
        sources = sources.filter(s => s.id !== id);
        renderSources();
    }
}

document.getElementById('addSourceForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('sourceName').value;
    const url = document.getElementById('sourceUrl').value;

    const newSource = await api.createSource(name, url);
    sources.push(newSource);
    renderSources();

    e.target.reset();
});

document.addEventListener('DOMContentLoaded', loadSources);