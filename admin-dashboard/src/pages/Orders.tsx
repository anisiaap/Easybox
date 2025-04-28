import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { api } from '../api';
import AdminEasyboxPickerDialog from './AdminEasyboxPickerDialog'; // adjust path!

const OrdersContainer = styled.div`
    padding: 20px;
`;

const Table = styled.table`
    width: 100%;
    border-collapse: collapse;
    background-color: #fff;
`;

const Th = styled.th`
    text-align: left;
    padding: 12px;
    background-color: #f0f0f0;
    border-bottom: 1px solid #ddd;
`;

const Td = styled.td`
    padding: 12px;
    border-bottom: 1px solid #ddd;
`;

const Button = styled.button`
  margin-right: 8px;
  background-color: #007bff;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #0056b3;
  }
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
                    <tr key={order.id}>
                        <Td>{order.id}</Td>
                        <Td>{order.userPhone}</Td>
                        <Td>{order.bakeryName}</Td>
                        <Td>{order.easyboxAddress}</Td>
                        <Td>
                            {editingId === order.id ? (
                                <>
                                    <div>
                                        <input
                                            value={editStatus}
                                            onChange={(e) => setEditStatus(e.target.value)}
                                        />
                                    </div>
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
                    </tr>
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
