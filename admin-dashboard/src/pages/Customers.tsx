import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { api } from '../api';

const Container = styled.div`
    padding: 32px;
    max-width: 800px;
    margin: 0 auto;
`;

const Card = styled.div`
    background-color: #ffffff;
    border-radius: 12px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
    padding: 20px;
    margin-bottom: 16px;
`;

const Title = styled.h1`
    font-size: 28px;
    margin-bottom: 24px;
`;

const Label = styled.p`
    margin: 4px 0;
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

const Form = styled.form`
    margin-top: 40px;
    background: #f9f9f9;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
`;

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
        <Container>
            <Title>Customers</Title>

            {customers.map((c) => (
                <Card key={c.id}>
                    {editingId === c.id ? (
                        <div>
                            <Input
                                value={editData.name}
                                onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                                placeholder="Name"
                            />
                            <Input
                                value={editData.phoneNumber}
                                onChange={(e) => setEditData({ ...editData, phoneNumber: e.target.value })}
                                placeholder="Phone Number"
                            />
                            <ButtonGroup>
                                <Button onClick={() => handleSaveEdit(c.id)}>Save</Button>
                                <Button onClick={() => setEditingId(null)}>Cancel</Button>
                            </ButtonGroup>
                        </div>
                    ) : (
                        <div>
                            <h2>{c.name}</h2>
                            <Label><strong>Phone:</strong> {c.phoneNumber}</Label>
                            <ButtonGroup>
                                <Button onClick={() => handleStartEdit(c)}>Edit</Button>
                                <Button onClick={() => handleDelete(c.id)}>Delete</Button>
                            </ButtonGroup>
                        </div>
                    )}
                </Card>
            ))}

            <Form onSubmit={handleCreate}>
                <h3>Add New Customer</h3>
                <Input
                    value={newCustomer.name}
                    onChange={(e) => setNewCustomer({ ...newCustomer, name: e.target.value })}
                    placeholder="Name"
                />
                <Input
                    value={newCustomer.phoneNumber}
                    onChange={(e) => setNewCustomer({ ...newCustomer, phoneNumber: e.target.value })}
                    placeholder="Phone Number"
                />
                <Button type="submit">Create</Button>
            </Form>
        </Container>
    );
};

export default Customers;
