const loginForm = document.getElementById('loginForm');
const loginBtn = document.getElementById('loginBtn');
const errorMessage = document.getElementById('errorMessage');

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
}

function hideError() {
    errorMessage.style.display = 'none';
}

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideError();

    const usernameOrEmail = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    loginBtn.disabled = true;
    loginBtn.textContent = 'Logging in...';

    try {
        await api.login(usernameOrEmail, password);
        window.location.href = 'dashboard.html';
    } catch (error) {
        showError(error.message || 'Login failed. Please check your credentials.');
        loginBtn.disabled = false;
        loginBtn.textContent = 'Login';
    }
});