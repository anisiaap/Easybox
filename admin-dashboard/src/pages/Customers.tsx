import React from 'react'
import styled from 'styled-components'

const CustomersContainer = styled.div`
  padding: 20px;
`
const CustomerItem = styled.div`
  background-color: #fff;
  margin-bottom: 10px;
  padding: 16px;
  border-radius: 8px;
`

const Customers: React.FC = () => {
    const customers = [
        { id: 1, phone: '555-1234', name: 'John Doe', totalOrders: 5 },
        { id: 2, phone: '555-5678', name: 'Jane Smith', totalOrders: 2 },
        { id: 3, phone: '555-9999', name: 'Michael Johnson', totalOrders: 10 },
    ]

    return (
        <CustomersContainer>
            <h1>Customers</h1>
            {customers.map(c => (
                <CustomerItem key={c.id}>
                    <h2>{c.name}</h2>
                    <p>Phone: {c.phone}</p>
                    <p>Total Orders: {c.totalOrders}</p>
                </CustomerItem>
            ))}
        </CustomersContainer>
    )
}

export default Customers
