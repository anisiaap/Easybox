import React from 'react'
import styled from 'styled-components'

const BakeriesContainer = styled.div`
  padding: 20px;
`
const BakeryCard = styled.div`
  background-color: #fff;
  margin-bottom: 10px;
  padding: 16px;
  border-radius: 8px;
`

const Bakeries: React.FC = () => {
    const bakeries = [
        { id: 'B1', name: 'Bakery A', location: '123 Main St', city: 'New York', owner: 'Alice' },
        { id: 'B2', name: 'Bakery B', location: '456 High St', city: 'New York', owner: 'Bob' },
    ]

    return (
        <BakeriesContainer>
            <h1>Bakeries</h1>
            {bakeries.map(b => (
                <BakeryCard key={b.id}>
                    <h2>{b.name}</h2>
                    <p><strong>Location:</strong> {b.location}, {b.city}</p>
                    <p><strong>Owner:</strong> {b.owner}</p>
                </BakeryCard>
            ))}
        </BakeriesContainer>
    )
}

export default Bakeries
