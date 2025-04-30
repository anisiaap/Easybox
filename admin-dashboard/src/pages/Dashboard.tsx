import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import KPICard from '../components/ui/KPICard';
import ChartPlaceholder from '../components/ui/ChartPlaceholder';
import { api } from '../api';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    LineChart, Line, PieChart, Pie, Cell, Legend
} from 'recharts';

const DashboardContainer = styled.div`
    padding: 40px;
    background: #f2f4f8;
    min-height: 100vh;
    width: 100%;
    box-sizing: border-box;
`;

const Header = styled.h1`
    font-size: 32px;
    color: #2d3436;
    margin-bottom: 30px;
    text-align: center;
`;

const KPIWrapper = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 40px;
`;

const SectionTitle = styled.h2`
    font-size: 24px;
    color: #2d3436;
    margin: 30px 0 20px;
`;

const ChartsGrid = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 40px;
    width: 100%;
`;

interface DashboardStats {
    totalEasyboxes: number;
    activeCompartments: number;
    totalOrders: number;
    expiredOrders: number;
}

interface OrdersTrendPoint {
    date: string;
    count: number;
}

interface OrdersStatusPoint {
    status: string;
    count: number;
}

interface CompartmentsStatus {
    free: number;
    busy: number;
}

const COLORS = ['#6c5ce7', '#00b894', '#fdcb6e', '#e17055'];

const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [ordersTrend, setOrdersTrend] = useState<OrdersTrendPoint[]>([]);
    const [ordersStatus, setOrdersStatus] = useState<OrdersStatusPoint[]>([]);
    const [compartmentsStatus, setCompartmentsStatus] = useState<CompartmentsStatus | null>(null);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            const statsRes = await api.get('/admin/dashboard/stats');
            setStats(statsRes.data);

            const trendRes = await api.get('/admin/dashboard/orders-trend');
            setOrdersTrend(trendRes.data);

            const statusRes = await api.get('/admin/dashboard/orders-status');
            setOrdersStatus(statusRes.data);

            const compartmentsRes = await api.get('/admin/dashboard/compartments-status');
            setCompartmentsStatus(compartmentsRes.data);
        } catch (err) {
            console.error('Error fetching dashboard data', err);
        }
    };

    const easyboxOccupancyData = compartmentsStatus
        ? [
            { name: 'Free Compartments', value: compartmentsStatus.free },
            { name: 'Busy Compartments', value: compartmentsStatus.busy }
        ]
        : [];

    return (
        <DashboardContainer>
            <Header>📊 Easybox Dashboard</Header>

            <KPIWrapper>
                <KPICard label="Total Easyboxes" value={stats?.totalEasyboxes ?? '-'} />
                <KPICard label="Active Compartments" value={stats?.activeCompartments ?? '-'} />
                <KPICard label="Orders" value={stats?.totalOrders ?? '-'} />
                <KPICard label="Expired Orders" value={stats?.expiredOrders ?? '-'} />
            </KPIWrapper>

            <SectionTitle>Analytics</SectionTitle>
            <ChartsGrid>
                <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={ordersTrend}>
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip />
                        <Line type="monotone" dataKey="count" stroke="#6c5ce7" strokeWidth={3} />
                    </LineChart>
                </ResponsiveContainer>

                <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                        <Pie
                            data={ordersStatus}
                            dataKey="count"
                            nameKey="status"
                            cx="50%"
                            cy="50%"
                            outerRadius={90}
                            label
                        >
                            {ordersStatus.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                        </Pie>
                        <Tooltip />
                        <Legend />
                    </PieChart>
                </ResponsiveContainer>

                <ResponsiveContainer width="100%" height={300}>
                    <BarChart layout="vertical" data={easyboxOccupancyData}>
                        <XAxis type="number" />
                        <YAxis dataKey="name" type="category" />
                        <Tooltip />
                        <Bar dataKey="value" fill="#00b894" barSize={24} />
                    </BarChart>
                </ResponsiveContainer>

                <ChartPlaceholder />
            </ChartsGrid>
        </DashboardContainer>
    );
};

export default Dashboard;
