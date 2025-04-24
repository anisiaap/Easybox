import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import Dashboard from './pages/Dashboard'
import Easyboxes from './pages/Easyboxes'
import Orders from './pages/Orders'
import Customers from './pages/Customers'
import Bakeries from './pages/Bakeries'
import AdminSettings from './pages/AdminSettings'

const App: React.FC = () => {
    return (
        <Router>
            <MainLayout>
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/easyboxes" element={<Easyboxes />} />
                    <Route path="/orders" element={<Orders />} />
                    <Route path="/customers" element={<Customers />} />
                    <Route path="/bakeries" element={<Bakeries />} />
                    <Route path="/admin-settings" element={<AdminSettings />} />
                </Routes>
            </MainLayout>
        </Router>
    )
}

export default App
