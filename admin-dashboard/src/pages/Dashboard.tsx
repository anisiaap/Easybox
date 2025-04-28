import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import KPICard from '../components/ui/KPICard';
import { api } from '../api';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

const DashboardContainer = styled.div`
    padding: 20px;
`;

const KPIWrapper = styled.div`
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
`;

interface DashboardStats {
    totalEasyboxes: number;
    activeCompartments: number;
    totalOrders: number;
    expiredOrders: number;
}

const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<DashboardStats | null>(null);

    useEffect(() => {
        fetchStats();
    }, []);

    const fetchStats = async () => {
        try {
            const res = await api.get('/admin/dashboard/stats');
            setStats(res.data);
        } catch (err) {
            console.error('Failed to fetch dashboard stats', err);
        }
    };

    const chartData = [
        { name: 'Orders', value: stats?.totalOrders || 0 },
        { name: 'Expired', value: stats?.expiredOrders || 0 },
        { name: 'Easyboxes', value: stats?.totalEasyboxes || 0 },
        { name: 'Compartments', value: stats?.activeCompartments || 0 }
    ];

    return (
        <DashboardContainer>
            <h1>Dashboard Overview</h1>
            <KPIWrapper>
                <KPICard label="Total Easyboxes" value={stats?.totalEasyboxes ?? '-'} />
                <KPICard label="Active Compartments" value={stats?.activeCompartments ?? '-'} />
                <KPICard label="Orders" value={stats?.totalOrders ?? '-'} />
                <KPICard label="Expired Orders" value={stats?.expiredOrders ?? '-'} />
            </KPIWrapper>

            <h2>Quick Overview</h2>
            <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                    <BarChart data={chartData}>
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Bar dataKey="value" />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </DashboardContainer>
    );
};

export default Dashboard;
