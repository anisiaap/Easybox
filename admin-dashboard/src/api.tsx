import axios from 'axios';
import { toast } from 'react-toastify';

export const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000
});

// Inject Authorization header if admin token is set in .env
api.interceptors.request.use(config => {
    const adminToken = process.env.REACT_APP_ADMIN_JWT;
    if (adminToken) {
        config.headers.Authorization = `Bearer ${adminToken}`;
    }
    return config;
});

/* Global error handler */
api.interceptors.response.use(
    res => res,
    err => {
        const { response } = err;
        const msg =
            response?.data?.message ||
            response?.statusText ||
            'Network / server error';
        toast.error(msg, { autoClose: 4000 });
        return Promise.reject(err);
    }
);
