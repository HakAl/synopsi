function checkAuth() {
    const isAuthPage = window.location.pathname.endsWith('index.html') ||
                       window.location.pathname.endsWith('register.html') ||
                       window.location.pathname === '/';

    if (!api.isAuthenticated() && !isAuthPage) {
        window.location.href = 'index.html';
    }
}

function logout() {
    api.logout();
}

document.addEventListener('DOMContentLoaded', () => {
    const logoutBtn = document.getElementById('logout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            logout();
        });
    }
});