import axios, { AxiosError } from 'axios';
import { getToken, saveToken, removeToken } from './auth';

const api = axios.create({
    baseURL: 'https://api.easybox-food.xyz/api/app',
});

// Add token to all requests
api.interceptors.request.use(async config => {
    const token = await getToken();
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config;

        console.log('[Interceptor] Received error:', err?.response?.status, originalRequest.url);

        if (
            err.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes('/auth/refresh-token') &&
            !originalRequest.url?.includes('/auth/login')
        ) {
            originalRequest._retry = true;

            try {
                console.log('[Interceptor] Attempting token refresh...');

                const oldToken = await getToken();
                console.log('[Interceptor] Old token:', oldToken);

                const refreshRes = await api.get('/auth/refresh-token', {
                    headers: {
                        Authorization: `Bearer ${oldToken}`,
                    },
                });

                const newToken = refreshRes.data;
                console.log('[Interceptor] Got new token:', newToken);

                await saveToken(newToken);

                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${newToken}`;

                return api(originalRequest);
            } catch (refreshErr) {
                const errTyped = refreshErr as AxiosError;

                console.error(
                    '[Interceptor] Refresh failed:',
                    errTyped.response?.data || errTyped.message
                );
                await removeToken();
                return Promise.reject(errTyped);
            }
        }

        return Promise.reject(err);
    }
);
export default api;