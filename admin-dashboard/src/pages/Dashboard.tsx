import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import KPICard from '../components/ui/KPICard';
import ChartPlaceholder from '../components/ui/ChartPlaceholder';
import { api } from '../api';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, Legend, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar
} from 'recharts';
import toast from 'react-hot-toast';
// Styled Components
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
    grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
    gap: 32px;
    width: 100%;
`;

const ChartCard = styled.div`
    background: #ffffff;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
    display: flex;
    flex-direction: column;
    align-items: center;
`;

const ChartTitle = styled.h3`
    font-size: 18px;
    color: #2d3436;
    margin-bottom: 16px;
`;

// Types
interface DashboardStats {
    totalEasyboxes: number;
    activeCompartments: number;
    totalOrders: number;
    expiredOrders: number;
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

// Component
const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [ordersStatus, setOrdersStatus] = useState<OrdersStatusPoint[]>([]);
    const [compartmentsStatus, setCompartmentsStatus] = useState<CompartmentsStatus | null>(null);
    const [radarData, setRadarData] = useState([]);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            const statsRes = await api.get('/admin/dashboard/stats');
            setStats(statsRes.data);

            const radarRes = await api.get('/admin/dashboard/compartments-by-condition');
            setRadarData(radarRes.data);

            const statusRes = await api.get('/admin/dashboard/orders-status');
            setOrdersStatus(statusRes.data);

            const compartmentsRes = await api.get('/admin/dashboard/compartments-status');
            setCompartmentsStatus(compartmentsRes.data);
        }
        catch (error: any) {
            const message = error?.response?.data || 'Error fetching dashboard data';
            toast.error(message);
            console.error('Error fetching dashboard data', error);
        }
    };

    const easyboxOccupancyData = compartmentsStatus
        ? [
            { name: 'Free Compartments', value: compartmentsStatus.free },
            { name: 'Busy Compartments', value: compartmentsStatus.busy },
        ]
        : [];

    return (
        <DashboardContainer>
            <Header>Easybox Dashboard</Header>

            <KPIWrapper>
                <KPICard label="Total Easyboxes" value={stats?.totalEasyboxes ?? '-'} />
                <KPICard label="Active Compartments" value={stats?.activeCompartments ?? '-'} />
                <KPICard label="Orders" value={stats?.totalOrders ?? '-'} />
                <KPICard label="Expired Orders" value={stats?.expiredOrders ?? '-'} />
            </KPIWrapper>

            <SectionTitle>Analytics</SectionTitle>
            <ChartsGrid>
                <ChartPlaceholder />
                {/* Line Chart - Orders Over Time */}
                <ChartCard>
                    <ChartTitle>Compartment Conditions by Easybox</ChartTitle>
                    <ResponsiveContainer width="100%" height={350}>
                        <RadarChart data={radarData}>
                            <PolarGrid />
                            {/*<PolarAngleAxis dataKey="easybox" />*/}
                            {/*<PolarRadiusAxis />*/}
                            <Radar name="Good" dataKey="good" stroke="#00b894" fill="#00b894" fillOpacity={0.6} />
                            <Radar name="Dirty" dataKey="dirty" stroke="#fdcb6e" fill="#fdcb6e" fillOpacity={0.6} />
                            <Radar name="Broken" dataKey="broken" stroke="#d63031" fill="#d63031" fillOpacity={0.6} />
                            <Legend />
                            <Tooltip />
                        </RadarChart>
                    </ResponsiveContainer>
                </ChartCard>
                {/* Pie Chart - Orders Status */}
                <ChartCard>
                    <ChartTitle>Orders by Status</ChartTitle>
                    <ResponsiveContainer width="100%" height={280}>
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
                </ChartCard>

                {/* Bar Chart - Compartment Occupancy */}
                <ChartCard>
                    <ChartTitle>Compartment Occupancy</ChartTitle>
                    <ResponsiveContainer width="100%" height={280}>
                        <BarChart layout="vertical" data={easyboxOccupancyData}>
                            <XAxis type="number" stroke="#999" />
                            <YAxis dataKey="name" type="category" stroke="#999" />
                            <Tooltip />
                            <Bar dataKey="value" barSize={30}>
                                {easyboxOccupancyData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </ChartCard>
            </ChartsGrid>
        </DashboardContainer>
    );
};

export default Dashboard;
