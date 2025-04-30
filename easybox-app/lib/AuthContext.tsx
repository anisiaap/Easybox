import React, { createContext, useContext, useState, useEffect } from 'react';

type AuthContextType = {
    userId: number;
    role: 'bakery' | 'client' | 'cleaner';
    setAuth: (userId: number, role: 'bakery' | 'client' | 'cleaner') => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [userId, setUserId] = useState<number>(1); // temporary default
    const [role, setRole] = useState<'bakery' | 'client' | 'cleaner'>('client');

    const setAuth = (id: number, r: 'bakery' | 'client' | 'cleaner') => {
        setUserId(id);
        setRole(r);
    };

    return (
        <AuthContext.Provider value={{ userId, role, setAuth }}>
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
