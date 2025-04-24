import React from 'react'
import styled from 'styled-components'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts'

// Example data
const data = [
    { name: 'Mon', orders: 40 },
    { name: 'Tue', orders: 55 },
    { name: 'Wed', orders: 60 },
    { name: 'Thu', orders: 70 },
    { name: 'Fri', orders: 90 },
    { name: 'Sat', orders: 65 },
    { name: 'Sun', orders: 50 }
]

const ChartContainer = styled.div`
  background-color: #fff;
  margin-top: 16px;
  padding: 20px;
  border-radius: 8px;
  width: 600px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
`

const ChartPlaceholder: React.FC = () => {
    return (
        <ChartContainer>
            <h3>Weekly Orders</h3>
            <LineChart width={550} height={300} data={data}>
                <CartesianGrid stroke="#ccc" strokeDasharray="5 5" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="orders" stroke="#82ca9d" strokeWidth={2} />
            </LineChart>
        </ChartContainer>
    )
}

export default ChartPlaceholder
