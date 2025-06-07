import axios, { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios';
import { toast } from 'react-hot-toast';

export const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000,
});/* 2 ──────────────────────────────────────────────────────────────── */
// cfg param typed as AxiosRequestConfig
api.interceptors.request.use((cfg: AxiosRequestConfig) => {
    const token = sessionStorage.getItem('token');
    if (token) {
        cfg.headers = cfg.headers ?? {};
        cfg.headers.Authorization = `Bearer ${token}`;
    }
    return cfg;
});

/* 3 ──────────────────────────────────────────────────────────────── */
export interface JwtPayload { exp: number }
export function decodeJwt(token: string): JwtPayload | null {
    try {
        const b64 = token.split('.')[1];
        const base64 = b64.replace(/-/g, '+').replace(/_/g, '/')
            .padEnd(Math.ceil(b64.length / 4) * 4, '=');
        return JSON.parse(atob(base64));
    } catch {
        return null;
    }
}
function hardLogout() {
    sessionStorage.removeItem('token');
    toast.error('Session expired. Please log in again.');
    setTimeout(() => window.location.assign('/login'), 200);
}

/* ------------------------------------------------------------------ */
/* 4. Global error / refresh-token handler                            */
/* ------------------------------------------------------------------ */
api.interceptors.response.use(
    (res: AxiosResponse) => res,
    async (err: AxiosError) => {
        const { response, config } = err;

        if (
            response?.status === 401 &&
            !config?._retry &&
            !config?.url?.includes('/auth/refresh-token') &&
            !config?.url?.includes('/auth/login')
        ) {
            (config as any)._retry = true;
            /* refresh flow ...  */
        }

        toast.error(
            response?.data?.message ??
            response?.statusText ??
            'Network / server error'
        );
        return Promise.reject(err);
    }
);

/* ------------------------------------------------------------------ */
/* 5. Proactive refresh 1 minute before expiry                        */
/* ------------------------------------------------------------------ */
export function scheduleTokenRefresh() {
    const token = sessionStorage.getItem('token');
    if (!token) return;

    const p = decodeJwt(token);
    if (!p?.exp) return;

    const ms = p.exp * 1000 - Date.now() - 60_000;
    if (ms <= 0) return;                 // already expiring; let 401 flow handle it

    setTimeout(async () => {
        try {
            const { data: newToken } = await api.get('/auth/refresh-token');
            sessionStorage.setItem('token', newToken);
            scheduleTokenRefresh();          // chain next refresh
        } catch {
            hardLogout();
        }
    }, ms);
}