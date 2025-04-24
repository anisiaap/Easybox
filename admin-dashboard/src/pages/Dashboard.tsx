import React from 'react'
import styled from 'styled-components'
import KPICard from '../components/ui/KPICard'
import ChartPlaceholder from '../components/ui/ChartPlaceholder'

const DashboardContainer = styled.div`
  padding: 20px;
`

const KPIWrapper = styled.div`
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
`

const Dashboard: React.FC = () => {
    return (
        <DashboardContainer>
            <h1>Dashboard Overview</h1>
            <KPIWrapper>
                <KPICard label="Total Easyboxes" value={54} />
                <KPICard label="Active Compartments" value={268} />
                <KPICard label="Orders Today" value={210} />
                <KPICard label="Expired Orders" value={7} />
            </KPIWrapper>
            <ChartPlaceholder />
        </DashboardContainer>
    )
}

export default Dashboard
