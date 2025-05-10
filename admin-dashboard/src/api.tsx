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
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 🧯 Global error handler
api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config;

        // Prevent infinite loop
        if (err.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                const refreshRes = await api.get('/auth/refresh-token');
                const newToken = refreshRes.data;
                localStorage.setItem('token', newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest); // retry
            } catch (refreshErr) {
                toast.error('Session expired');
                localStorage.removeItem('token');
            }
        }

        toast.error(
            err.response?.data?.message || err.response?.statusText || 'Network / server error'
        );
        return Promise.reject(err);
    }
);