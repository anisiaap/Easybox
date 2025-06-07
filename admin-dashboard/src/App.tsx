
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
import React, { useEffect } from 'react';
import {decodeJwt, scheduleTokenRefresh} from './api';


const App: React.FC = () => {
    useEffect(() => {
        const token = sessionStorage.getItem('token');
        if (token) {
            const payload = decodeJwt(token);
            if (payload?.exp && payload.exp * 1000 > Date.now()) {
                scheduleTokenRefresh();
            } else {
                sessionStorage.removeItem('token'); // expired token
            }
        }
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
