import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from 'recharts';
import { api } from '../../api';

interface WeeklyDataPoint {
    day: string;
    orders: number;
}

const ChartCard = styled.div`
    background: #ffffff;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
`;

const ChartTitle = styled.h3`
    font-size: 18px;
    color: #2d3436;
    margin-bottom: 16px;
`;

const ChartPlaceholder: React.FC = () => {
    const [data, setData] = useState<WeeklyDataPoint[]>([]);

    useEffect(() => {
        const fetchWeeklyData = async () => {
            try {
                const res = await api.get('/admin/dashboard/orders-weekly');
                setData(res.data);
            } catch (err) {
                console.error('Failed to fetch weekly orders:', err);
            }
        };
        fetchWeeklyData();
    }, []);

    return (
        <ChartCard>
            <ChartTitle>Weekly Orders</ChartTitle>
            <ResponsiveContainer width="100%" height={300}>
                <LineChart data={data}>
                    <CartesianGrid stroke="#ccc" strokeDasharray="5 5" />
                    <XAxis dataKey="day" stroke="#999" />
                    <YAxis stroke="#999" />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="orders" stroke="#82ca9d" strokeWidth={3} dot={{ r: 4 }} />
                </LineChart>
            </ResponsiveContainer>
        </ChartCard>
    );
};

export default ChartPlaceholder;
