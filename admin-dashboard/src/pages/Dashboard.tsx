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
    padding: 20px;
`;

const KPIWrapper = styled.div`
    display: flex;
    gap: 20px;
    margin-bottom: 30px;
    flex-wrap: wrap;
`;

const ChartsGrid = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 30px;
`;

interface DashboardStats {
    totalEasyboxes: number;
    activeCompartments: number;
    totalOrders: number;
    expiredOrders: number;
}

interface OrdersTrendPoint {
    date: string; // like "2025-04-28"
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

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

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
            <h1>Dashboard Overview</h1>

            {/* KPI Cards */}
            <KPIWrapper>
                <KPICard label="Total Easyboxes" value={stats?.totalEasyboxes ?? '-'} />
                <KPICard label="Active Compartments" value={stats?.activeCompartments ?? '-'} />
                <KPICard label="Orders" value={stats?.totalOrders ?? '-'} />
                <KPICard label="Expired Orders" value={stats?.expiredOrders ?? '-'} />
            </KPIWrapper>

            {/* Charts Section */}
            <h2>Analytics</h2>
            <ChartsGrid>

                {/* Line Chart: Orders Trend */}
                <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={ordersTrend}>
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip />
                        <Line type="monotone" dataKey="count" stroke="#82ca9d" strokeWidth={2} />
                    </LineChart>
                </ResponsiveContainer>

                {/* Pie Chart: Orders by Status */}
                <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                        <Pie
                            data={ordersStatus}
                            dataKey="count"
                            nameKey="status"
                            cx="50%"
                            cy="50%"
                            outerRadius={80}
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

                {/* Horizontal Bar: Easybox occupancy */}
                <ResponsiveContainer width="100%" height={300}>
                    <BarChart layout="vertical" data={easyboxOccupancyData}>
                        <XAxis type="number" />
                        <YAxis dataKey="name" type="category" />
                        <Tooltip />
                        <Bar dataKey="value" fill="#00c49f" />
                    </BarChart>
                </ResponsiveContainer>

                {/* Placeholder for new future charts */}
                <ChartPlaceholder />

            </ChartsGrid>
        </DashboardContainer>
    );
};

export default Dashboard;
