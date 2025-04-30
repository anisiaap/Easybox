// lib/AuthContext.ts
import React, { createContext, useContext, useState } from 'react';
import { removeToken } from './auth';

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

    const setAuth = (userInfo: UserInfo) => {
        setUser(userInfo);
    };

    const logout = async () => {
        await removeToken();
        setUser(null);
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
