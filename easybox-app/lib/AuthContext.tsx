import React, { createContext, useContext, useState, useRef } from 'react';
import { jwtDecode } from 'jwt-decode';
import api from './api';
import { getToken, saveToken, removeToken } from './auth';

type UserInfo = {
    userId: number;
    name: string;
    phone: string;
    role: 'bakery' | 'client' | 'cleaner';
};

type AuthContextType = {
    user: UserInfo | null;
    setAuth: (user: UserInfo) => void;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<UserInfo | null>(null);
    const refreshTimeout = useRef<number | null>(null);
    const scheduleTokenRefresh = (token: string) => {
        try {
            const decoded: any = jwtDecode(token);
            const expiry = decoded.exp * 1000;
            const refreshTime = expiry - Date.now() - 60_000;

            if (refreshTime > 0) {
                refreshTimeout.current = setTimeout(async () => {
                    try {
                        const res = await api.get('/auth/refresh-token');
                        const newToken = res.data;
                        await saveToken(newToken);
                        scheduleTokenRefresh(newToken); // 🔁 continue refreshing
                    } catch (err) {
                        await logout();
                    }
                }, refreshTime);
            }
        } catch (err) {
            console.error('Invalid token:', err);
            logout();
        }
    };

    const setAuth = (userInfo: UserInfo) => {
        setUser(userInfo);
        getToken().then(token => {
            if (token) scheduleTokenRefresh(token);
        });
    };

    const logout = async () => {
        if (refreshTimeout.current) {
            clearTimeout(refreshTimeout.current);
            refreshTimeout.current = null;
        }
        await removeToken();
        setUser(null);
        // Optional: navigate to login screen or show alert
    };

    return (
        <AuthContext.Provider value={{ user, setAuth, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};