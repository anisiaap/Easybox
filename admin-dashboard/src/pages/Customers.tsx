import React, { useState, useEffect, useCallback } from 'react';
import styled from 'styled-components';
import { api } from '../api';
import toast from 'react-hot-toast';
import ConfirmDialog from '../components/ui/ConfirmDialog';
const Table = styled.table`
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    background-color: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    overflow: hidden;
    margin-bottom: 32px;
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
`;

const TableRow = styled.tr`
    &:hover {
        background-color: #f0f8f0;
    }
`;

const Container = styled.div`
    padding: 32px;
    max-width: 800px;
    margin: 0 auto;
`;



const Title = styled.h1`
    font-size: 28px;
    margin-bottom: 24px;
`;

const ButtonGroup = styled.div`
    margin-top: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
`;

const Button = styled.button`
    background-color: #28a745;
    color: white;
    border: none;
    padding: 8px 14px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s ease;
    &:hover {
        background-color: #218838;
    }
`;

const Input = styled.input`
    width: 100%;
    margin-bottom: 12px;
    padding: 10px;
    border-radius: 6px;
    border: 1px solid #ccc;
`;
//
// const Form = styled.form`
//     margin-top: 40px;
//     background: #f9f9f9;
//     padding: 24px;
//     border-radius: 12px;
//     box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
// `;

interface Customer {
    id: number;
    name: string;
    phoneNumber: string;
}

const Customers: React.FC = () => {
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editData, setEditData] = useState<Omit<Customer, 'id'>>({ name: '', phoneNumber: '' });
    // const [newCustomer, setNewCustomer] = useState<Omit<Customer, 'id'>>({ name: '', phoneNumber: '' });
    const [page, setPage] = useState(0);
    const pageSize = 10;
    const [totalUsers, setTotalUsers] = useState(0);
    type ConfirmAction = null | {
        message: string;
        onConfirm: () => void;
    };
    const [searchName, setSearchName] = useState('');
    const [searchPhone, setSearchPhone] = useState('');
    const [allCustomers, setAllCustomers] = useState<Customer[]>([]);
    const [confirmDialog, setConfirmDialog] = useState<ConfirmAction>(null);
    const fetchCustomers = useCallback(async () => {
        try {
            const params = new URLSearchParams({
                page: page.toString(),
                size: pageSize.toString()
            });
            const [usersRes, countRes] = await Promise.all([
                api.get(`/admin/users?${params}`),
                api.get(`/admin/users/count?${params}`),
            ]);
            setCustomers(usersRes.data);
            setCustomers(usersRes.data);
            setTotalUsers(countRes.data);
        } catch (error: any) {
            toast.error(error?.response?.data || 'Failed to fetch customers');
        }
    }, [page ]);

    useEffect(() => {
        let filtered = allCustomers;

        if (searchName.trim()) {
            filtered = filtered.filter(c => c.name.toLowerCase().includes(searchName.toLowerCase()));
        }

        if (searchPhone.trim()) {
            filtered = filtered.filter(c => c.phoneNumber.includes(searchPhone));
        }

        setCustomers(filtered);
        setPage(0); // reset pagination
    }, [searchName, searchPhone, allCustomers]);



    const handleDelete = (id: number) => {
        setConfirmDialog({
            message: 'Are you sure you want to delete this customer? This action cannot be undone.',
            onConfirm: async () => {
                try {
                    await api.delete(`/admin/users/${id}`);
                    fetchCustomers();
                    toast.success('Customer deleted');
                } catch (err) {
                    toast.error('Failed to delete customer');
                }
                setConfirmDialog(null);
            },
        });
    };

    const handleStartEdit = (customer: Customer) => {
        setEditingId(customer.id);
        setEditData({ name: customer.name, phoneNumber: customer.phoneNumber });
    };

    const handleSaveEdit = (id: number) => {
        setConfirmDialog({
            message: 'Are you sure you want to save the changes to this customer?',
            onConfirm: async () => {
                try {
                    await api.put(`/admin/users/${id}`, editData);
                    setEditingId(null);
                    fetchCustomers();
                    toast.success('Customer updated');
                } catch (err) {
                    toast.error('Failed to update customer');
                }
                setConfirmDialog(null);
            },
        });
    };

    // const handleCreate = async (e: React.FormEvent) => {
    //     e.preventDefault();
    //     try {
    //         await api.post('/admin/users', newCustomer);
    //         setNewCustomer({ name: '', phoneNumber: '' });
    //         toast.success('Customer created successfully!');
    //         fetchCustomers();
    //     }catch (error: any) {
    //         const message = error?.response?.data || 'Failed to create customer';
    //         toast.error(message);
    //         console.error('Error creating customer', error);
    //     }
    // };

    return (
        <Container>
            <Title>Customers</Title>
            <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <Input
                    placeholder="Search Name"
                    value={searchName}
                    onChange={(e) => setSearchName(e.target.value)}
                />
                <Input
                    placeholder="Search Phone"
                    value={searchPhone}
                    onChange={(e) => setSearchPhone(e.target.value)}
                />
                <Button onClick={() => { setPage(0); fetchCustomers(); }}>Search</Button>
            </div>

            <Table>
                <thead>
                <tr>
                    <Th>Name</Th>
                    <Th>Phone</Th>
                    <Th>Actions</Th>
                </tr>
                </thead>
                <tbody>
                {customers.filter(c => c.name !== null).map((c)  => (
                    <TableRow key={c.id}>
                        <Td>
                            {editingId === c.id ? (
                                <Input
                                    value={editData.name}
                                    onChange={(e) =>
                                        setEditData({ ...editData, name: e.target.value })
                                    }
                                    placeholder="Name"
                                />
                            ) : (
                                c.name
                            )}
                        </Td>
                        {/*<Td>*/}
                        {/*    {editingId === c.id ? (*/}
                        {/*        <Input*/}
                        {/*            value={editData.phoneNumber}*/}
                        {/*            onChange={(e) =>*/}
                        {/*                setEditData({ ...editData, phoneNumber: e.target.value })*/}
                        {/*            }*/}
                        {/*            placeholder="Phone Number"*/}
                        {/*        />*/}
                        {/*    ) : (*/}
                        {/*        c.phoneNumber*/}
                        {/*    )}*/}
                        {/*</Td>*/}
                        <Td>{c.phoneNumber}</Td>
                        <Td>
                            <ButtonGroup>
                                {editingId === c.id ? (
                                    <>
                                        <Button onClick={() => handleSaveEdit(c.id)}>Save</Button>
                                        <Button onClick={() => setEditingId(null)}>Cancel</Button>
                                    </>
                                ) : (
                                    <>
                                        <Button onClick={() => handleStartEdit(c)}>Edit</Button>
                                        <Button onClick={() => handleDelete(c.id)}>Delete</Button>
                                    </>
                                )}
                            </ButtonGroup>
                        </Td>
                    </TableRow>
                ))}
                </tbody>
            </Table>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginBottom: '20px' }}>
                <Button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</Button>
                <span>Page {page + 1} of {Math.ceil(totalUsers / pageSize)}</span>
                <Button onClick={() => setPage(p => (p + 1 < Math.ceil(totalUsers / pageSize) ? p + 1 : p))}
                        disabled={(page + 1) >= Math.ceil(totalUsers / pageSize)}>Next</Button>
            </div>

            {/*<Form onSubmit={handleCreate}>*/}
            {/*    <h3>Add New Customer</h3>*/}
            {/*    <Input*/}
            {/*        value={newCustomer.name}*/}
            {/*        onChange={(e) => setNewCustomer({ ...newCustomer, name: e.target.value })}*/}
            {/*        placeholder="Name"*/}
            {/*    />*/}
            {/*    <Input*/}
            {/*        value={newCustomer.phoneNumber}*/}
            {/*        onChange={(e) => setNewCustomer({ ...newCustomer, phoneNumber: e.target.value })}*/}
            {/*        placeholder="Phone Number"*/}
            {/*    />*/}
            {/*    <Button type="submit">Create</Button>*/}
            {/*</Form>*/}
            {confirmDialog && (
                <ConfirmDialog
                    message={confirmDialog.message}
                    onConfirm={confirmDialog.onConfirm}
                    onCancel={() => setConfirmDialog(null)}
                />
            )}
        </Container>
    );
};

export default Customers;
