const forgotPasswordForm = document.getElementById('forgotPasswordForm');
const resetBtn = document.getElementById('resetBtn');
const errorMessage = document.getElementById('errorMessage');
const successMessage = document.getElementById('successMessage');

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
    successMessage.style.display = 'none';
}

function showSuccess(message) {
    successMessage.textContent = message;
    successMessage.style.display = 'block';
    errorMessage.style.display = 'none';
}

function hideMessages() {
    errorMessage.style.display = 'none';
    successMessage.style.display = 'none';
}

forgotPasswordForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideMessages();

    const email = document.getElementById('email').value;

    resetBtn.disabled = true;
    resetBtn.textContent = 'Sending...';

    try {
        await api.requestPasswordReset(email);
        showSuccess('If an account exists with that email, password reset instructions have been sent. Check your console logs for the reset token.');
        forgotPasswordForm.reset();
    } catch (error) {
        showError(error.message || 'Unable to process password reset request');
    } finally {
        resetBtn.disabled = false;
        resetBtn.textContent = 'Send Reset Link';
    }
});