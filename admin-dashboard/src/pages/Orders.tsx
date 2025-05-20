import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { api } from '../api';
import AdminEasyboxPickerDialog from './AdminEasyboxPickerDialog'; // adjust path!
import toast from 'react-hot-toast';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { useCallback } from 'react';// Adjust path if needed
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
    background-color: #28a745;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 0.9rem;
    cursor: pointer;
    transition: background-color 0.2s ease;
    &:hover {
        background-color: #28a745;
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
const statusOptions = [
    { label: 'Pending', value: 'pending' },
    { label: 'Confirmed', value: 'confirmed' },
    { label: 'Waiting for Bakery Dropoff', value: 'waiting_for_bakery_dropoff' },
    { label: 'Pickup Order', value: 'pickup_order' },
    { label: 'Waiting Cleaning', value: 'waiting_cleaning' },
    { label: 'Expired', value: 'expired' },
    { label: 'Completed', value: 'completed' },
];

const Orders: React.FC = () => {
    const [orders, setOrders] = useState<Order[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editStatus, setEditStatus] = useState<string>('');
    const [editEasybox, setEditEasybox] = useState<{ id: number; address: string } | null>(null);
    const [showEasyboxPicker, setShowEasyboxPicker] = useState(false);
    const [filterDate, setFilterDate] = useState('');
    const [filterBakeryId, setFilterBakeryId] = useState('');
    const [filterUserId, setFilterUserId] = useState('');

    type ConfirmAction = null | {
        message: string;
        onConfirm: () => void;
    };
    const [page, setPage] = useState(0);
    const pageSize = 10;
    const [totalOrders, setTotalOrders] = useState(0);

    const [confirmDialog, setConfirmDialog] = useState<ConfirmAction>(null);
    const fetchOrders = useCallback(async () => {
        try {
            const query = new URLSearchParams({
                page: page.toString(),
                size: pageSize.toString(),
                ...(filterDate && { deliveryDate: filterDate }),
                ...(filterBakeryId && { bakeryId: filterBakeryId }),
                ...(filterUserId && { userId: filterUserId }),
            }).toString();

            const [ordersRes, countRes] = await Promise.all([
                api.get(`/admin/reservations?${query}`),
                api.get(`/admin/reservations/count`)
            ]);
            setOrders(ordersRes.data);
            setTotalOrders(countRes.data);
        } catch (error: any) {
            const message = error?.response?.data || 'Failed to fetch orders';
            toast.error(message);
            console.error('Failed to fetch orders', error);
        }
    }, [page, filterDate, filterBakeryId, filterUserId]);

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);


    const handleDeleteOrder = (id: number) => {
        setConfirmDialog({
            message: 'Are you sure you want to delete this order? This action cannot be undone.',
            onConfirm: async () => {
                try {
                    await api.delete(`/admin/reservations/${id}`);
                    fetchOrders();
                    toast.success('Order deleted');
                } catch (error) {
                    toast.error('Error deleting order');
                }
                setConfirmDialog(null);
            }
        });
    };

    const handleStartEdit = (order: Order) => {
        setEditingId(order.id);
        setEditStatus(order.status); // much cleaner and accurate
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
    const handleSaveEdit = (id: number) => {
        setConfirmDialog({
            message: 'Are you sure you want to save the changes to this order?',
            onConfirm: async () => {
                try {
                    await api.put(`/admin/reservations/${id}`, {
                        status: editStatus,
                        easyboxId: editEasybox?.id
                    });
                    setEditingId(null);
                    setEditEasybox(null);
                    fetchOrders();
                    toast.success('Order updated');
                } catch (error) {
                    toast.error('Error updating order');
                }
                setConfirmDialog(null);
            }
        });
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
                                    <select
                                        value={editStatus}
                                        onChange={(e) => setEditStatus(e.target.value)}
                                        style={{
                                            padding: '8px',
                                            borderRadius: '6px',
                                            border: '1px solid #ccc',
                                            width: '100%',
                                            maxWidth: '200px'
                                        }}
                                    >
                                        {statusOptions.map(({ label, value }) => (
                                            <option key={value} value={value}>
                                                {label}
                                            </option>
                                        ))}
                                    </select>
                                    {/*<div style={{ marginTop: 8 }}>*/}
                                    {/*    <b>Easybox:</b> {editEasybox?.address || 'None selected'}*/}
                                    {/*    <br />*/}
                                    {/*    <Button onClick={handleChangeEasybox}>Change Easybox</Button>*/}
                                    {/*</div>*/}
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
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginBottom: '20px' }}>
                <Button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</Button>
                <span>Page {page + 1} of {Math.ceil(totalOrders / pageSize)}</span>
                <Button onClick={() => setPage(p => (p + 1 < Math.ceil(totalOrders / pageSize) ? p + 1 : p))}
                        disabled={(page + 1) >= Math.ceil(totalOrders / pageSize)}>Next</Button>
            </div>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <Input
                    type="date"
                    value={filterDate}
                    onChange={e => setFilterDate(e.target.value)}
                    placeholder="Filter by date"
                />
                <Input
                    type="number"
                    value={filterBakeryId}
                    onChange={e => setFilterBakeryId(e.target.value)}
                    placeholder="Bakery ID"
                />
                <Input
                    type="number"
                    value={filterUserId}
                    onChange={e => setFilterUserId(e.target.value)}
                    placeholder="Client/User ID"
                />
                <Button onClick={() => fetchOrders()}>Apply Filters</Button>
            </div>
            {showEasyboxPicker && (
                <AdminEasyboxPickerDialog
                    onSelect={handleEasyboxSelected}
                    onClose={() => setShowEasyboxPicker(false)}
                />
            )}
            {confirmDialog && (
                <ConfirmDialog
                    message={confirmDialog.message}
                    onConfirm={confirmDialog.onConfirm}
                    onCancel={() => setConfirmDialog(null)}
                />
            )}
        </OrdersContainer>
    );
};

export default Orders;
