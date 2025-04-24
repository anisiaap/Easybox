// src/api.js
import axios from 'axios';
import { toast } from 'react-toastify';

export const api = axios.create({
    baseURL:'http://localhost:8080/api/', // <-- matches your back-end prefix
    timeout: 10000
});

/* Global response interceptor */
api.interceptors.response.use(
    res => res,
    err => {
        const { response } = err;
        const msg =
            response?.data?.message ||
            response?.statusText    ||
            'Network / server error';

        toast.error(msg, { autoClose: 4000 });
        return Promise.reject(err);            // propagate so callers may .catch
    }
);
