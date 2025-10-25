const registerForm = document.getElementById('registerForm');
const registerBtn = document.getElementById('registerBtn');
const errorMessage = document.getElementById('errorMessage');

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
}

function hideError() {
    errorMessage.style.display = 'none';
}

registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideError();

    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    if (password.length < 8) {
        showError('Password must be at least 8 characters');
        return;
    }

    registerBtn.disabled = true;
    registerBtn.textContent = 'Creating account...';

    try {
        await api.register(username, email, password);
        window.location.href = 'dashboard.html';
    } catch (error) {
        showError(error.message || 'Registration failed. Please try again.');
        registerBtn.disabled = false;
        registerBtn.textContent = 'Register';
    }
});