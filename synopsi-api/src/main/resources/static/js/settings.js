checkAuth();

    const changePasswordForm = document.getElementById('changePasswordForm');
    const changePasswordBtn = document.getElementById('changePasswordBtn');
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

    document.getElementById('logout').addEventListener('click', (e) => {
        e.preventDefault();
        logout();
    });

    changePasswordForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideMessages();

        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmNewPassword = document.getElementById('confirmNewPassword').value;

        if (newPassword !== confirmNewPassword) {
            showError('New passwords do not match');
            return;
        }

        if (newPassword.length < 8) {
            showError('Password must be at least 8 characters');
            return;
        }

        changePasswordBtn.disabled = true;
        changePasswordBtn.textContent = 'Updating...';

        try {
            await api.changePassword(currentPassword, newPassword);
            showSuccess('Password updated successfully!');
            changePasswordForm.reset();
        } catch (error) {
            showError(error.message || 'Failed to update password');
        } finally {
            changePasswordBtn.disabled = false;
            changePasswordBtn.textContent = 'Update Password';
        }
    });