import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { api } from '../api';
import AdminEasyboxPickerDialog from './AdminEasyboxPickerDialog'; // adjust path!
const OrdersContainer = styled.div`
    padding: 20px;
    background: #f4f6f5;
    min-height: 100vh;
`;

const Table = styled.table`
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    background-color: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    overflow: hidden;
`;

const Th = styled.th`
    text-align: left;
    padding: 16px;
    background-color: #5e5e5e;
    color: white;
    border-bottom: 1px solid #ddd;
    font-weight: 600;
`;

const Td = styled.td`
    padding: 16px;
    border-bottom: 1px solid #eee;
    vertical-align: top;
`;

const TableRow = styled.tr`
    &:hover {
        background-color: #f0f8f0;
    }
`;

const Button = styled.button`
    margin: 4px;
    background-color: #104208;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 0.9rem;
    cursor: pointer;
    transition: background-color 0.2s ease;
    &:hover {
        background-color: #0d3318;
    }
    &:disabled {
        background-color: #aaa;
        cursor: not-allowed;
    }
`;

const Input = styled.input`
    padding: 6px 10px;
    border-radius: 4px;
    border: 1px solid #ccc;
    width: 100%;
    max-width: 200px;
`;


// Interfaces
interface Order {
    id: number;
    userPhone: string;
    bakeryName: string;
    easyboxAddress: string;
    status: string;
    easyboxId?: number; // ADD THIS
}


const Orders: React.FC = () => {
    const [orders, setOrders] = useState<Order[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editStatus, setEditStatus] = useState<string>('');
    const [editEasybox, setEditEasybox] = useState<{ id: number; address: string } | null>(null);
    const [showEasyboxPicker, setShowEasyboxPicker] = useState(false);

    useEffect(() => {
        fetchOrders();
    }, []);

    const fetchOrders = async () => {
        try {
            const response = await api.get('/admin/reservations');
            setOrders(response.data);
        } catch (error) {
            console.error('Error fetching orders', error);
        }
    };

    const handleDeleteOrder = async (id: number) => {
        try {
            await api.delete(`/admin/reservations/${id}`);
            fetchOrders();
        } catch (error) {
            console.error('Error deleting order', error);
        }
    };

    const handleStartEdit = (order: Order) => {
        setEditingId(order.id);
        setEditStatus(order.status);
        if (order.easyboxId && order.easyboxAddress) {
            setEditEasybox({ id: order.easyboxId, address: order.easyboxAddress });
        } else {
            setEditEasybox(null);
        }
    };


    const handleChangeEasybox = () => {
        setShowEasyboxPicker(true);
    };
    const handleEasyboxSelected = (box: { id: number; address: string }) => {
        setEditEasybox({ id: box.id, address: box.address });
        setShowEasyboxPicker(false);
    };
    const handleSaveEdit = async (id: number) => {
        try {
            await api.put(`/admin/reservations/${id}`, {
                status: editStatus,
                easyboxId: editEasybox?.id
            });
            setEditingId(null);
            setEditEasybox(null);
            fetchOrders();
        } catch (error) {
            console.error('Error updating order', error);
        }
    };

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
                    <Th>Actions</Th>
                </tr>
                </thead>
                <tbody>
                {orders.map(order => (
                    <TableRow key={order.id}>
                        <Td>{order.id}</Td>
                        <Td>{order.userPhone}</Td>
                        <Td>{order.bakeryName}</Td>
                        <Td>{order.easyboxAddress}</Td>
                        <Td>
                            {editingId === order.id ? (
                                <>
                                    <Input
                                        value={editStatus}
                                        onChange={(e) => setEditStatus(e.target.value)}
                                    />
                                    <div style={{ marginTop: 8 }}>
                                        <b>Easybox:</b> {editEasybox?.address || 'None selected'}
                                        <br />
                                        <Button onClick={handleChangeEasybox}>Change Easybox</Button>
                                    </div>
                                </>
                            ) : (
                                order.status
                            )}
                        </Td>
                        <Td>
                            {editingId === order.id ? (
                                <>
                                    <Button onClick={() => handleSaveEdit(order.id)}>Save</Button>
                                    <Button onClick={() => { setEditingId(null); setEditEasybox(null); }}>Cancel</Button>
                                </>
                            ) : (
                                <>
                                    <Button onClick={() => handleStartEdit(order)}>Edit</Button>
                                    <Button onClick={() => handleDeleteOrder(order.id)}>Delete</Button>
                                </>
                            )}
                        </Td>
                    </TableRow>
                ))}
                </tbody>
            </Table>
            {showEasyboxPicker && (
                <AdminEasyboxPickerDialog
                    onSelect={handleEasyboxSelected}
                    onClose={() => setShowEasyboxPicker(false)}
                />
            )}
        </OrdersContainer>
    );
};

export default Orders;
