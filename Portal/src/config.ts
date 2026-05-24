// Central configuration for the Portal backend URL
// This automatically reads the VITE_API_BASE_URL env variable (useful for production Vercel builds)
// and defaults to localhost:8082 for offline/local development.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082';
