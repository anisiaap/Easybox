import axios from 'axios';
import { toast } from 'react-hot-toast';

export const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000,
});

// ✅ Attach JWT from localStorage before every request
api.interceptors.request.use(config => {
    const token = sessionStorage.getItem('token');
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
export function decodeJwt(token: string): { exp: number } | null {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload;
    } catch (err) {
        console.error("Invalid JWT format:", err);
        return null;
    }
}

// 🧯 Global error handler
api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config;

        // 401: Try refresh token once
        if (
            err.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes('/auth/refresh-token')&&
            !originalRequest.url?.includes('/auth/login')
        ) {
            originalRequest._retry = true;

            try {
                const refreshRes = await api.get('/auth/refresh-token');
                const newToken = refreshRes.data;

                sessionStorage.setItem('token', newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;

                return api(originalRequest); // Retry original request
            } catch (refreshErr) {
                // 🔐 Auto logout
                sessionStorage.removeItem('token');
                toast.error('Session expired. Please log in again.');
                if (typeof window !== 'undefined') {
                    window.location.href = '/login';
                }


                return Promise.reject(refreshErr);
            }
        }

        // General error
        toast.error(
            err.response?.data?.message || err.response?.statusText || 'Network / server error'
        );
        return Promise.reject(err);
    }
);
// Schedule token refresh
export const scheduleTokenRefresh = () => {
    const token = sessionStorage.getItem('token');
    if (!token) return;

    const payload = decodeJwt(token);
    if (!payload?.exp) return;

    const refreshInMs = payload.exp * 1000 - Date.now() - 60_000; // Refresh 1 minute before expiration

    if (refreshInMs > 0) {
        setTimeout(async () => {
            try {
                const res = await api.get('/auth/refresh-token');
                const newToken = res.data;
                sessionStorage.setItem('token', newToken);
                scheduleTokenRefresh(); // Schedule the next refresh
            } catch (err) {
                console.warn('Failed to refresh token:', err);
                sessionStorage.removeItem('token');
                window.location.href = '/login';
            }
        }, refreshInMs);
    }
};