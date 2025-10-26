// API Configuration
const API_BASE_URL = 'http://localhost:8080';
const TOKEN_KEY = 'synopsi_jwt_token';
const USER_KEY = 'synopsi_user';

// Token Management
const tokenManager = {
    setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
    getToken: () => localStorage.getItem(TOKEN_KEY),
    removeToken: () => localStorage.removeItem(TOKEN_KEY),

    setUser: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
    getUser: () => {
        const user = localStorage.getItem(USER_KEY);
        return user ? JSON.parse(user) : null;
    },
    removeUser: () => localStorage.removeItem(USER_KEY),

    clear: () => {
        tokenManager.removeToken();
        tokenManager.removeUser();
    }
};

// Parse JWT to extract userId
const parseJwt = (token) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
};

// HTTP Client with automatic token injection
const httpClient = async (url, options = {}) => {
    const token = tokenManager.getToken();

    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    };

    // Add Authorization header if token exists and not a public endpoint
    if (token && !url.includes('/api/v1/auth/')) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, config);

        // Handle 401 - token expired or invalid
        // Handle 403 - forbidden/insufficient permissions
        if (response.status === 401 || response.status === 403) {
            tokenManager.clear();
            window.location.href = '/index.html';
            throw new Error('Session expired. Please login again.');
        }

        // Handle other error status codes
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw {
                status: response.status,
                message: errorData.message || errorData.errorMessage || `HTTP ${response.status}`,
                data: errorData
            };
        }

        // Handle 204 No Content
        if (response.status === 204) {
            return null;
        }

        return await response.json();
    } catch (error) {
        // Network errors or other fetch failures
        if (!error.status) {
            throw {
                status: 0,
                message: 'Network error. Please check your connection.',
                data: error
            };
        }
        throw error;
    }
};

// API Methods
const api = {
    // ==================== Auth ====================
    login: async (usernameOrEmail, password) => {
        const response = await httpClient('/api/v1/auth/login', {
            method: 'POST',
            body: JSON.stringify({ usernameOrEmail, password })
        });

        // Store token and user info
        tokenManager.setToken(response.token);
        const payload = parseJwt(response.token);
        tokenManager.setUser({
            id: payload.userId,
            username: payload.sub
        });

        return response;
    },

    register: async (username, email, password) => {
        const response = await httpClient('/api/v1/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, email, password })
        });

        // Store token and user info
        tokenManager.setToken(response.token);
        const payload = parseJwt(response.token);
        tokenManager.setUser({
            id: payload.userId,
            username: payload.sub
        });

        return response;
    },

    logout: () => {
        tokenManager.clear();
        window.location.href = '/index.html';
    },

    requestPasswordReset: async (email) => {
        return await httpClient('/api/v1/auth/password-reset', {
            method: 'POST',
            body: JSON.stringify({ email })
        });
    },

    confirmPasswordReset: async (token, newPassword) => {
        return await httpClient('/api/v1/auth/password-reset/confirm', {
            method: 'POST',
            body: JSON.stringify({ token, newPassword })
        });
    },

    // ==================== Topics ====================
    getAllTopics: async (params = {}) => {
        const query = new URLSearchParams(params).toString();
        return await httpClient(`/api/v1/topics${query ? '?' + query : ''}`);
    },

    getTopicById: async (id, includeChildren = false) => {
        return await httpClient(`/api/v1/topics/${id}?includeChildren=${includeChildren}`);
    },

    getTopicBySlug: async (slug) => {
        return await httpClient(`/api/v1/topics/by-slug/${slug}`);
    },

    getRootTopics: async (includeChildren = false) => {
        return await httpClient(`/api/v1/topics/root?includeChildren=${includeChildren}`);
    },

    getChildTopics: async (parentId) => {
        return await httpClient(`/api/v1/topics/${parentId}/children`);
    },

    createTopic: async (topicData) => {
        return await httpClient('/api/v1/topics', {
            method: 'POST',
            body: JSON.stringify(topicData)
        });
    },

    updateTopic: async (id, topicData) => {
        return await httpClient(`/api/v1/topics/${id}`, {
            method: 'PUT',
            body: JSON.stringify(topicData)
        });
    },

    deleteTopic: async (id) => {
        return await httpClient(`/api/v1/topics/${id}`, {
            method: 'DELETE'
        });
    },

    // ==================== Sources ====================
    getAllSources: async (params = {}) => {
        const query = new URLSearchParams(params).toString();
        return await httpClient(`/api/v1/sources${query ? '?' + query : ''}`);
    },

    getSourceById: async (id) => {
        return await httpClient(`/api/v1/sources/${id}`);
    },

    getSourceByIdWithFeeds: async (id) => {
        return await httpClient(`/api/v1/sources/${id}/with-feeds`);
    },

    getSourceByName: async (name) => {
        return await httpClient(`/api/v1/sources/by-name/${name}`);
    },

    createSource: async (sourceData) => {
        return await httpClient('/api/v1/sources', {
            method: 'POST',
            body: JSON.stringify(sourceData)
        });
    },

    updateSource: async (id, sourceData) => {
        return await httpClient(`/api/v1/sources/${id}`, {
            method: 'PUT',
            body: JSON.stringify(sourceData)
        });
    },

    activateSource: async (id) => {
        return await httpClient(`/api/v1/sources/${id}/activate`, {
            method: 'PATCH'
        });
    },

    deactivateSource: async (id) => {
        return await httpClient(`/api/v1/sources/${id}/deactivate`, {
            method: 'PATCH'
        });
    },

    deleteSource: async (id) => {
        return await httpClient(`/api/v1/sources/${id}`, {
            method: 'DELETE'
        });
    },

    // ==================== Personalization ====================
    getPersonalizedFeed: async (page = 0, size = 20) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(
            `/api/v1/personalization/feed/${user.id}?page=${page}&size=${size}&sort=relevanceScore,desc`
        );
    },

    recordReadingInteraction: async (articleId, timeSpentSeconds) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/personalization/interactions/${user.id}/read`, {
            method: 'POST',
            body: JSON.stringify({ articleId, timeSpentSeconds })
        });
    },

    recordFeedback: async (articleId, feedbackType) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/personalization/interactions/${user.id}/feedback`, {
            method: 'POST',
            body: JSON.stringify({ articleId, feedbackType })
        });
    },

    getUserPreferences: async () => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/personalization/preferences/${user.id}`);
    },

    updateUserPreference: async (preferenceData) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/personalization/preferences/${user.id}`, {
            method: 'PUT',
            body: JSON.stringify(preferenceData)
        });
    },

    getInferredInterests: async () => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/personalization/interests/${user.id}`);
    },

    getArticle: async (articleId) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(
            `/api/v1/articles/${articleId}`
        );
    },

    getSimilarArticles: async (articleId, limit = 10) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(
            `/api/v1/personalization/similar/${user.id}/${articleId}?limit=${limit}`
        );
    },

    // ==================== Users ====================
    getCurrentUser: async () => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/users/${user.id}`);
    },

    getUserWithStats: async () => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/users/${user.id}/stats`);
    },

    updateCurrentUser: async (updateData) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/users/${user.id}`, {
            method: 'PUT',
            body: JSON.stringify(updateData)
        });
    },

    changePassword: async (currentPassword, newPassword) => {
        const user = tokenManager.getUser();
        if (!user) throw new Error('User not authenticated');

        return await httpClient(`/api/v1/users/${user.id}/password`, {
            method: 'PUT',
            body: JSON.stringify({ currentPassword, newPassword })
        });
    },

    checkEmailExists: async (email) => {
        return await httpClient(`/api/v1/users/check/email?email=${encodeURIComponent(email)}`);
    },

    checkUsernameExists: async (username) => {
        return await httpClient(`/api/v1/users/check/username?username=${encodeURIComponent(username)}`);
    },

    // ==================== Utility ====================
    isAuthenticated: () => {
        return !!tokenManager.getToken();
    },

    getCurrentUserId: () => {
        const user = tokenManager.getUser();
        return user ? user.id : null;
    }
};

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
}