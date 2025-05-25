
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import Dashboard from './pages/Dashboard'
import Easyboxes from './pages/Easyboxes'
import Orders from './pages/Orders'
import Customers from './pages/Customers'
import Bakeries from './pages/Bakeries'
import { Toaster } from 'react-hot-toast';
import LoginPage from "./pages/LoginPage";
import ProtectedRoute from './pages/ProtectedRoute';
import { jwtDecode } from 'jwt-decode';
import React, { useEffect } from 'react';
import { decodeJwt, api } from './api';

const App: React.FC = () => {
    useEffect(() => {
        const checkAndRefreshToken = async () => {
            const token = localStorage.getItem('token');
            if (!token) return;

            const payload = decodeJwt(token);
            if (!payload?.exp) return;

            const now = Math.floor(Date.now() / 1000);
            const secondsLeft = payload.exp - now;

            if (secondsLeft < 120) {
                try {
                    const res = await api.get('/auth/refresh-token');
                    const newToken = res.data;
                    localStorage.setItem('token', newToken);
                    console.log('🔄 Token refreshed');
                } catch (err) {
                    console.warn('⚠️ Failed to refresh token', err);
                    localStorage.removeItem('token');
                    window.location.href = '/login';
                }
            }
        };

        const interval = setInterval(checkAndRefreshToken, 30_000);
        return () => clearInterval(interval);
    }, []);
    return (
        <Router>
            <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route
                    path="/"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <Dashboard />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/easyboxes"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <Easyboxes />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/orders"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <Orders />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/customers"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <Customers />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/bakeries"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <Bakeries />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </Router>

    )
}

export default App
