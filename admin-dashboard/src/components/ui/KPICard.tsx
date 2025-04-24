import React from 'react'
import styled from 'styled-components'

const CardContainer = styled.div`
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  width: 200px;
  text-align: center;
`

const KPIValue = styled.div`
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 8px;
`

const KPILabel = styled.div`
  font-size: 0.9rem;
  color: #888;
`

interface KPICardProps {
    label: string
    value: number | string
}

const KPICard: React.FC<KPICardProps> = ({ label, value }) => {
    return (
        <CardContainer>
            <KPIValue>{value}</KPIValue>
            <KPILabel>{label}</KPILabel>
        </CardContainer>
    )
}

export default KPICard
