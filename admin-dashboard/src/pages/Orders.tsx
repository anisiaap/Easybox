import React, { useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import { api } from '../api';
import AdminEasyboxPickerDialog from './AdminEasyboxPickerDialog';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import toast from 'react-hot-toast';
import type { EasyboxDto } from '../types/easybox'; // adjust path as needed
/* ───────────────────────────────────  styled helpers  ─────────────────────────────────── */
const OrdersContainer = styled.div`padding:20px;background:#f4f6f5;min-height:100vh;`;
const Table           = styled.table`width:100%;border-collapse:separate;border-spacing:0;background:#fff;
    border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.05);overflow:hidden;`;
const Th              = styled.th`text-align:left;padding:16px;background:#5e5e5e;color:#fff;
    border-bottom:1px solid #ddd;font-weight:600;`;
const Td              = styled.td`padding:16px;border-bottom:1px solid #eee;vertical-align:top;`;
const TableRow        = styled.tr`&:hover{background:#f0f8f0;}`;
const Button          = styled.button`margin:4px;background:#28a745;color:#fff;border:none;padding:6px 12px;
    border-radius:6px;font-size:.9rem;cursor:pointer;transition:.2s;
    &:hover{background:#28a745;} &:disabled{background:#aaa;cursor:not-allowed;}`;
const Input           = styled.input`padding:6px 10px;border-radius:4px;border:1px solid #ccc;width:100%;max-width:200px;`;

/* ───────────────────────────────────────  types  ─────────────────────────────────────── */
interface Order {
    id: number;
    userPhone: string;
    bakeryName: string;
    easyboxAddress: string;
    easyboxId?: number;
    status: string;
}



const statusOptions = [
    { label: 'Pending',                     value: 'pending' },
    { label: 'Confirmed',                   value: 'confirmed' },
    { label: 'Waiting for Bakery Dropoff',  value: 'waiting_bakery_drop_off' },
    { label: 'Pickup Order',               value: 'waiting_client_pick_up' },
    { label: 'Waiting Cleaning',            value: 'waiting_cleaning' },
    { label: 'Expired',                     value: 'expired' },
    { label: 'Completed',                   value: 'completed' },
];

/* ───────────────────────────────────── component  ────────────────────────────────────── */
export default function Orders() {
    /* ------------ data/state ------------ */
    const [orders, setOrders] = useState<Order[]>([]);
    const [page, setPage] = useState(0);
    const [totalOrders, setTotalOrders] = useState(0);
    const pageSize = 10;

    /* filters */
    const [filterDate, setFilterDate] = useState('');
    const [filterBakeryName, setFilterBakeryName] = useState('');
    const [filterUserPhone, setFilterUserPhone] = useState('');

    /* row-edit state */
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editStatus, setEditStatus] = useState('');
    const [editEasybox, setEditEasybox] = useState<{ id: number; address: string } | null>(null);

    /* picker/dialog state */
    const [pickerBoxes, setPickerBoxes] = useState<EasyboxDto[]>([]);
    const [pickerOrderId, setPickerOrderId] = useState<number | null>(null);
    const [showEasyboxPicker, setShowEasyboxPicker] = useState(false);
    const [confirmDialog, setConfirmDialog] = useState<{
        message: string; onConfirm: () => void
    } | null>(null);

    /* ------------ fetch orders ------------ */
    const fetchOrders = useCallback(async () => {
        try {
            const query = new URLSearchParams({
                page: page.toString(),
                size: pageSize.toString(),
                ...(filterDate && {deliveryDate: filterDate}),
                ...(filterBakeryName && {bakeryName: filterBakeryName}),
                ...(filterUserPhone && {userPhone: filterUserPhone}),
            }).toString();

            const [list, count] = await Promise.all([
                api.get(`/admin/reservations?${query}`),
                api.get(`/admin/reservations/count?${query}`),
            ]);
            setOrders(list.data);
            setTotalOrders(count.data);
        } catch (err: any) {
            toast.error(err?.response?.data || 'Failed to fetch orders');
        }
    }, [page, filterDate, filterBakeryName, filterUserPhone]);

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);

    /* ------------ CRUD helpers ------------ */
    const handleDeleteOrder = (id: number) =>
        setConfirmDialog({
            message: 'Are you sure you want to delete this order?',
            onConfirm: async () => {
                try {
                    await api.delete(`/admin/reservations/${id}`);
                    fetchOrders();
                } catch {
                    toast.error('Delete failed');
                }
                setConfirmDialog(null);
            }
        });

    const handleStartEdit = (o: Order) => {
        setEditingId(o.id);
        setEditStatus(o.status);
        setEditEasybox(o.easyboxId ? {id: o.easyboxId, address: o.easyboxAddress} : null);
    };

    const handleSaveEdit = async (id: number) => {
        setConfirmDialog({
            message: 'Save changes?',
            onConfirm: async () => {
                try {
                    await api.put(`/admin/reservations/${id}`, {
                        status: editStatus,
                        easyboxId: editEasybox?.id          // undefined if unchanged
                    });
                    toast.success('Order updated');
                    setEditingId(null);
                    setEditEasybox(null);
                    fetchOrders();
                } catch {
                    toast.error('Update failed');
                }
                setConfirmDialog(null);
            }
        });
    };

    /* ------------ easybox picker flow ------------ */
    const openPickerForOrder = async (order: Order) => {
        try {
            const {data} = await api.get(`/admin/reservations/${order.id}/available-easyboxes`);
            const list: EasyboxDto[] = [
                ...(data.exact ? [data.exact] : []),
                ...(data.others ? data.others : []),
            ];
            setPickerBoxes(list);
            setPickerOrderId(order.id);
            setShowEasyboxPicker(true);
        } catch {
            toast.error('Failed to load alternative easyboxes');
        }
    };

    const handleEasyboxSelected = async (box: EasyboxDto) => {
        if (!pickerOrderId) return;
        try {
            await api.put(`/admin/reservations/${pickerOrderId}/reassign-easybox`,
                null, {params: {newEasyboxId: box.id}});
            toast.success('Easybox reassigned ✅');
            setShowEasyboxPicker(false);
            if (editingId === pickerOrderId) {
                setEditingId(null);
                setEditEasybox(null);
            }
            fetchOrders();
        } catch {
            toast.error('Reassign failed');
        }
    };

    /* ───────────────────────────── render ───────────────────────────── */
    return (
        <OrdersContainer>
            <h1>Orders</h1>

            {/* ------------ table ------------ */}
            <Table>
                <thead>
                <tr>
                    <Th>Order ID</Th><Th>User Phone</Th><Th>Bakery</Th><Th>Easybox</Th>
                    <Th>Status / Edit</Th><Th>Actions</Th>
                </tr>
                </thead>
                <tbody>
                {orders.map(o => (
                    <TableRow key={o.id}>
                        <Td>{o.id}</Td>
                        <Td>{o.userPhone}</Td>
                        <Td>{o.bakeryName}</Td>
                        <Td>{o.easyboxAddress}</Td>

                        {/* status + easybox edit cell */}
                        <Td>
                            {editingId === o.id ? (
                                <>
                                    <select value={editStatus}
                                            onChange={e => setEditStatus(e.target.value)}
                                            style={{padding: '8px', border: '1px solid #ccc'}}>
                                        {statusOptions.map(opt => (
                                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                                        ))}
                                    </select>
                                    <div style={{marginTop: 6}}>
                                        <b>Easybox:</b> {editEasybox?.address || 'None'}
                                        <br/>
                                        <Button onClick={() => openPickerForOrder(o)}>Change</Button>
                                    </div>
                                </>
                            ) : (<>{o.status}</>)}
                        </Td>

                        {/* action buttons */}
                        <Td>
                            {editingId === o.id
                                ? <>
                                    <Button onClick={() => handleSaveEdit(o.id)}>Save</Button>
                                    <Button onClick={() => {
                                        setEditingId(null);
                                        setEditEasybox(null);
                                    }}>Cancel</Button>
                                </>
                                : <>
                                    <Button onClick={() => handleStartEdit(o)}>Edit</Button>
                                    <Button onClick={() => handleDeleteOrder(o.id)}>Delete</Button>
                                </>
                            }
                        </Td>
                    </TableRow>
                ))}
                </tbody>
            </Table>

            {/* ------------ pagination ------------ */}
            <div style={{display: 'flex', justifyContent: 'center', gap: 12, margin: 20}}>
                <Button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</Button>
                <span>Page {page + 1} / {Math.ceil(totalOrders / pageSize)}</span>
                <Button onClick={() => setPage(p => p + 1 < Math.ceil(totalOrders / pageSize) ? p + 1 : p)}
                        disabled={page + 1 >= Math.ceil(totalOrders / pageSize)}>Next</Button>
            </div>

            {/* ------------ filters ------------ */}
            <div style={{display: 'flex', gap: 12, marginBottom: 20}}>
                <div>
                    <label style={{fontWeight: 500}}>Delivery Date</label>
                    <Input type="date" value={filterDate} onChange={e => setFilterDate(e.target.value)}/>
                </div>
                <Input value={filterBakeryName} onChange={e => setFilterBakeryName(e.target.value)}
                       placeholder="Bakery Name"/>
                <Input value={filterUserPhone} onChange={e => setFilterUserPhone(e.target.value)}
                       placeholder="User Phone"/>
                <Button onClick={fetchOrders}>Apply</Button>
            </div>

            {/* ------------ dialogs ------------ */}
            {showEasyboxPicker && pickerOrderId && (
                <AdminEasyboxPickerDialog
                    boxes={pickerBoxes}
                    onSelect={handleEasyboxSelected}
                    onClose={() => setShowEasyboxPicker(false)}
                />
            )}
            {confirmDialog && (
                <ConfirmDialog
                    message={confirmDialog.message}
                    onConfirm={confirmDialog.onConfirm}
                    onCancel={() => setConfirmDialog(null)}   // fallback
                />
            )}
        </OrdersContainer>
    );
}