// src/api.js
/* global __REACT_APP_API_URL__ */
import axios from 'axios';
import { toast } from 'react-toastify';

export const api = axios.create({
    baseURL: __REACT_APP_API_URL__,//"https://api.easybox-food.xyz/api/", //process.env.REACT_APP_API_URL, // <-- matches your back-end prefix
    timeout: 60000
});
let widgetJwt = null;
export function setWidgetJwt(token) {
    widgetJwt = token;
}
api.interceptors.request.use(
    config => {
        if (widgetJwt) {
            config.headers['Authorization'] = `Bearer ${widgetJwt}`;
        }
        return config;
    },
    error => Promise.reject(error)
);
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
