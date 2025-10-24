// API client stub - will be implemented with real endpoints in next step
const API_BASE_URL = '/api';

const api = {
    // Auth
    login: async (email, password) => {
        console.log('API: login', email);
        return { token: 'mock-jwt-token' };
    },

    register: async (email, password) => {
        console.log('API: register', email);
        return { success: true };
    },

    // Feeds
    getPersonalizedFeed: async () => {
        console.log('API: getPersonalizedFeed');
        return mockSummaries;
    },

    // Topics
    getAllTopics: async () => {
        console.log('API: getAllTopics');
        return mockTopics;
    },

    // Sources
    getAllSources: async () => {
        console.log('API: getAllSources');
        return mockSources;
    },

    createSource: async (name, url) => {
        console.log('API: createSource', name, url);
        return { id: Date.now(), name, url };
    },

    deleteSource: async (id) => {
        console.log('API: deleteSource', id);
        return { success: true };
    }
};

// Mock data for UI development
const mockTopics = [
    { id: 1, name: 'Technology', slug: 'technology' },
    { id: 2, name: 'Science', slug: 'science' },
    { id: 3, name: 'Business', slug: 'business' },
    { id: 4, name: 'Health', slug: 'health' },
    { id: 5, name: 'Politics', slug: 'politics' }
];

const mockSources = [
    { id: 1, name: 'TechCrunch', url: 'https://techcrunch.com/feed/' },
    { id: 2, name: 'Hacker News', url: 'https://news.ycombinator.com/rss' }
];

const mockSummaries = [
    {
        id: 1,
        title: 'New AI Model Breaks Performance Records',
        source: 'TechCrunch',
        publishedAt: '2025-10-24T10:30:00Z',
        preview: 'A groundbreaking AI model has achieved unprecedented results in natural language understanding...',
        summary: 'Researchers have developed a new AI model that significantly outperforms previous benchmarks in natural language understanding tasks. The model uses a novel architecture that combines transformer-based attention mechanisms with advanced memory systems.',
        url: 'https://example.com/article/1',
        topics: ['Technology', 'AI']
    },
    {
        id: 2,
        title: 'Climate Study Reveals New Insights',
        source: 'Science Daily',
        publishedAt: '2025-10-24T08:15:00Z',
        preview: 'New research into climate patterns suggests more rapid changes than previously predicted...',
        summary: 'A comprehensive climate study spanning 20 years has revealed that certain environmental changes are occurring more rapidly than earlier models predicted. Scientists are calling for updated climate action strategies.',
        url: 'https://example.com/article/2',
        topics: ['Science', 'Environment']
    }
];