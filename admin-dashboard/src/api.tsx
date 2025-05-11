import axios from 'axios';
import { toast } from 'react-hot-toast';

export const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000,
});

// ✅ Attach JWT from localStorage before every request
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

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

                localStorage.setItem('token', newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;

                return api(originalRequest); // Retry original request
            } catch (refreshErr) {
                // 🔐 Auto logout
                localStorage.removeItem('token');
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