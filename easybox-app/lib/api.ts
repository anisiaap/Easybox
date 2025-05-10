import axios from 'axios';
import { getToken, saveToken, removeToken } from './auth';

const api = axios.create({
    baseURL: 'https://api.easybox-food.xyz/api/app',
});

api.interceptors.request.use(async (config) => {
    const token = await getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config;
        if (err.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshRes = await api.get('/auth/refresh-token');
                const newToken = refreshRes.data;
                await saveToken(newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest); // retry
            } catch (refreshErr) {
                await removeToken();
                // Optionally, redirect to login or notify
            }
        }

        return Promise.reject(err);
    }
);
export default api;
