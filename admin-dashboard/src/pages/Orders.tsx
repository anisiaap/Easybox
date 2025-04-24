import React from 'react'
import styled from 'styled-components'

const OrdersContainer = styled.div`
  padding: 20px;
`

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
`

const Th = styled.th`
  text-align: left;
  padding: 12px;
  background-color: #f0f0f0;
  border-bottom: 1px solid #ddd;
`

const Td = styled.td`
  padding: 12px;
  border-bottom: 1px solid #ddd;
`

const Orders: React.FC = () => {
    // Dummy orders
    const orders = [
        { id: 101, userPhone: '555-1234', bakery: 'Bakery A', easybox: 'Easybox #1', status: 'PENDING' },
        { id: 102, userPhone: '555-5678', bakery: 'Bakery B', easybox: 'Easybox #2', status: 'READY_FOR_PICKUP' },
        { id: 103, userPhone: '555-9876', bakery: 'Bakery A', easybox: 'Easybox #1', status: 'PICKED_UP' },
    ]

    return (
        <OrdersContainer>
            <h1>Orders</h1>
            <Table>
                <thead>
                <tr>
                    <Th>Order ID</Th>
                    <Th>User Phone</Th>
                    <Th>Bakery</Th>
                    <Th>Easybox</Th>
                    <Th>Status</Th>
                </tr>
                </thead>
                <tbody>
                {orders.map(order => (
                    <tr key={order.id}>
                        <Td>{order.id}</Td>
                        <Td>{order.userPhone}</Td>
                        <Td>{order.bakery}</Td>
                        <Td>{order.easybox}</Td>
                        <Td>{order.status}</Td>
                    </tr>
                ))}
                </tbody>
            </Table>
        </OrdersContainer>
    )
}

export default Orders
