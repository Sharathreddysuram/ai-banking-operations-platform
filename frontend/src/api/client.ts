import axios from 'axios';

export const TOKEN_KEY = 'aibop_token';
export const USERNAME_KEY = 'aibop_username';
export const ROLE_KEY = 'aibop_role';

/**
 * Axios instance for the Spring Cloud Gateway.
 * baseURL is empty by default so requests go through the Vite dev proxy
 * (or the nginx proxy in Docker) to http://localhost:8080.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(ROLE_KEY);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

/** Extract a human-readable message from any API failure. */
export function apiErrorMessage(error: unknown, fallback = 'Request failed. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | string | undefined;
    if (typeof data === 'string' && data.trim()) return data;
    if (data && typeof data === 'object' && (data.message || data.error)) {
      return data.message ?? data.error ?? fallback;
    }
    if (error.response) return `Server returned ${error.response.status} ${error.response.statusText}`;
    return 'Cannot reach the API gateway (http://localhost:8080). Is the backend running?';
  }
  if (error instanceof Error) return error.message;
  return fallback;
}

export default client;
