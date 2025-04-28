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
            <Routes>
                <Route
                    path="/"
                    element={
                        <MainLayout>
                            <Dashboard />
                        </MainLayout>
                    }
                />
                <Route
                    path="/easyboxes"
                    element={
                        <MainLayout>
                            <Easyboxes />
                        </MainLayout>
                    }
                />
                <Route
                    path="/orders"
                    element={
                        <MainLayout>
                            <Orders />
                        </MainLayout>
                    }
                />
                <Route
                    path="/customers"
                    element={
                        <MainLayout>
                            <Customers />
                        </MainLayout>
                    }
                />
                <Route
                    path="/bakeries"
                    element={
                        <MainLayout>
                            <Bakeries />
                        </MainLayout>
                    }
                />
                {/*<Route*/}
                {/*    path="/admin-settings"*/}
                {/*    element={*/}
                {/*        <MainLayout>*/}
                {/*            <AdminSettings />*/}
                {/*        </MainLayout>*/}
                {/*    }*/}
                {/*/>*/}
            </Routes>
        </Router>

    )
}

export default App
