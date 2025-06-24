import axios, { AxiosRequestConfig, AxiosResponse, AxiosError , InternalAxiosRequestConfig} from 'axios';
import { toast } from 'react-hot-toast';

export const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000,
});

//  Decode JWT
export function decodeJwt(token: string): { exp: number } | null {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload;
    } catch (err) {
        console.error("Invalid JWT format:", err);
        return null;
    }
}

// Force logout
export function forceLogout(): void {
    sessionStorage.removeItem('token');
    toast.error('Session expired. Please log in again.');
    window.location.href = '/login';
}

//  Attach token before requests


api.interceptors.request.use((config: InternalAxiosRequestConfig) => {

    const token = sessionStorage.getItem('token');
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

//  Global response error handler
api.interceptors.response.use(
    (res: AxiosResponse) => res,
    async (err: AxiosError): Promise<any> => {
        const originalRequest = err.config as AxiosRequestConfig & { _retry?: boolean };

        if (
            err.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes('/auth/refresh-token') &&
            !originalRequest.url?.includes('/auth/login')
        ) {
            originalRequest._retry = true;

            try {
                const res = await api.get('/auth/refresh-token');
                const newToken =
                    typeof res.data === 'string' ? res.data : res.data.token;
                sessionStorage.setItem('token', newToken);
                originalRequest.headers = {
                    ...originalRequest.headers,
                    Authorization: `Bearer ${newToken}`,
                };
                return api(originalRequest);
            } catch (refreshErr) {
                forceLogout();
                return Promise.reject(refreshErr);
            }
        }



        return Promise.reject(err);
    }
);

//  Token refresh scheduler
export const scheduleTokenRefresh = (): void => {
    const token = sessionStorage.getItem('token');
    if (!token) return;

    const payload = decodeJwt(token);
    if (!payload?.exp) return;

    const refreshInMs = payload.exp * 1000 - Date.now() - 60_000;

    const refresh = async () => {
        try {
            const res = await api.get('/auth/refresh-token');
            const newToken =
                typeof res.data === 'string' ? res.data : res.data.token;
            sessionStorage.setItem('token', newToken);
            scheduleTokenRefresh(); // Reschedule
        } catch (err) {
            console.warn('Token refresh failed:', err);
            forceLogout();
        }
    };

    if (refreshInMs <= 0) {
        refresh(); // Refresh immediately
    } else {
        setTimeout(refresh, refreshInMs);
    }
};
