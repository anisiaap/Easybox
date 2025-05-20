import React, {
    createContext,
    useContext,
    useState,
    useRef,
    useEffect,
} from 'react';
import { jwtDecode } from 'jwt-decode';
import api from './api';
import { getToken, saveToken, removeToken } from './auth';

const REFRESH_BUFFER_MS = 60_000; // ✱ CHANGE: moved to a named constant

interface JwtPayload {
    sub: number;
    name: string;
    exp: number;
    role: 'USER' | 'BAKERY';
} // ✱ CHANGE: explicit payload type

export type UserInfo = {
    userId: number;
    name: string;
    phone: string;
    role: 'bakery' | 'client' | 'cleaner';
    token: string;
};

type AuthContextType = {
    user: UserInfo | null | undefined;
    setAuth: (user: UserInfo) => void;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<UserInfo | null | undefined>(undefined);
    const refreshTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
    /** Clears any pending timeout before scheduling the next one. */
    const scheduleTokenRefresh = (token: string) => {
        if (refreshTimeout.current) clearTimeout(refreshTimeout.current as number); // ✱ CHANGE: prevent leaks

        try {
            const { exp } = jwtDecode<JwtPayload>(token); // ✱ CHANGE: typed decode
            const refreshInMs = exp * 1000 - Date.now() - REFRESH_BUFFER_MS;

            if (refreshInMs > 0) {
                refreshTimeout.current = setTimeout(async () => {
                    try {
                        const { data: newToken } = await api.get<string>('/auth/refresh-token');
                        await saveToken(newToken);
                        scheduleTokenRefresh(newToken); // 🔁 keep chain alive
                    } catch {
                        await logout();
                    }
                }, refreshInMs);
            }
        } catch (err) {
            console.error('Invalid token:', err);
            logout();
        }
    };

    const setAuth = (userInfo: UserInfo) => {
        setUser(userInfo);
        getToken().then(token => token && scheduleTokenRefresh(token));
    };

    const logout = async () => {
        if (refreshTimeout.current) clearTimeout(refreshTimeout.current);
        refreshTimeout.current = null;
        await removeToken();
        setUser(null);
    };

    // ✱ CHANGE: auto‑log‑in on cold start
    useEffect(() => {
        (async () => {
            const token = await getToken();
            if (token) {
                try {
                    const { exp } = jwtDecode<JwtPayload>(token);
                    if (exp * 1000 > Date.now()) {
                        const profile = await api.get('/auth/me').then(r => r.data);
                        setAuth({
                            userId: profile.userId,
                            name: profile.name,
                            phone: profile.phone,
                            role: profile.role,
                            token: profile.token || null
                        });
                        scheduleTokenRefresh(token);
                    } else {
                        await removeToken();
                    }
                } catch {
                    await removeToken();
                }
            }
        })();
    }, []);

    return (
        <AuthContext.Provider value={{ user, setAuth, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within an AuthProvider');
    return context;
};