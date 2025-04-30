// src/pages/Customers.tsx

import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { api } from '../api'; // your axios client

// Styled Components
const CustomersContainer = styled.div`
    padding: 20px;
`;

const CustomerCard = styled.div`
    background-color: #fff;
    margin-bottom: 10px;
    padding: 16px;
    border-radius: 8px;
    position: relative;
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

const Form = styled.form`
    margin-top: 20px;
    background: #fafafa;
    padding: 16px;
    border-radius: 8px;
`;

// Types
interface Customer {
    id: number;
    name: string;
    phoneNumber: string;
}

const Customers: React.FC = () => {
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editData, setEditData] = useState<Omit<Customer, 'id'>>({ name: '', phoneNumber: '' });
    const [newCustomer, setNewCustomer] = useState<Omit<Customer, 'id'>>({ name: '', phoneNumber: '' });

    useEffect(() => {
        fetchCustomers();
    }, []);

    const fetchCustomers = async () => {
        try {
            const res = await api.get('/admin/users');
            setCustomers(res.data);
        } catch (err) {
            console.error('Failed to fetch customers', err);
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await api.delete(`/admin/users/${id}`);
            fetchCustomers();
        } catch (err) {
            console.error('Delete error', err);
        }
    };

    const handleStartEdit = (customer: Customer) => {
        setEditingId(customer.id);
        setEditData({ name: customer.name, phoneNumber: customer.phoneNumber });
    };

    const handleSaveEdit = async (id: number) => {
        try {
            await api.put(`/admin/users/${id}`, editData);
            setEditingId(null);
            fetchCustomers();
        } catch (err) {
            console.error('Save error', err);
        }
    };

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await api.post('/admin/users', newCustomer);
            setNewCustomer({ name: '', phoneNumber: '' });
            fetchCustomers();
        } catch (err) {
            console.error('Create error', err);
        }
    };

    return (
        <CustomersContainer>
            <h1>Customers</h1>

            {/* List of Customers */}
            {customers.map((c) => (
                <CustomerCard key={c.id}>
                    {editingId === c.id ? (
                        <div>
                            <input
                                value={editData.name}
                                onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                                placeholder="Name"
                            /><br/>
                            <input
                                value={editData.phoneNumber}
                                onChange={(e) => setEditData({ ...editData, phoneNumber: e.target.value })}
                                placeholder="Phone Number"
                            /><br/>
                            <Button onClick={() => handleSaveEdit(c.id)}>Save</Button>
                            <Button onClick={() => setEditingId(null)}>Cancel</Button>
                        </div>
                    ) : (
                        <div>
                            <h2>{c.name}</h2>
                            <p><strong>Phone:</strong> {c.phoneNumber}</p>
                            <Button onClick={() => handleStartEdit(c)}>Edit</Button>
                            <Button onClick={() => handleDelete(c.id)}>Delete</Button>
                        </div>
                    )}
                </CustomerCard>
            ))}

            {/* Create New Customer Form */}
            <Form onSubmit={handleCreate}>
                <h3>Add New Customer</h3>
                <input
                    value={newCustomer.name}
                    onChange={(e) => setNewCustomer({ ...newCustomer, name: e.target.value })}
                    placeholder="Name"
                /><br/>
                <input
                    value={newCustomer.phoneNumber}
                    onChange={(e) => setNewCustomer({ ...newCustomer, phoneNumber: e.target.value })}
                    placeholder="Phone Number"
                /><br/>
                <Button type="submit">Create</Button>
            </Form>
        </CustomersContainer>
    );
};

export default Customers;
