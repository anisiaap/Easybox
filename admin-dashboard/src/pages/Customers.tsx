import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { api } from '../api';

const CustomersContainer = styled.div`
    padding: 20px;
`;

const CustomerItem = styled.div`
    background-color: #fff;
    margin-bottom: 10px;
    padding: 16px;
    border-radius: 8px;
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

// Types
interface Customer {
    id: number;
    name: string;
    phoneNumber: string;
}

const Customers: React.FC = () => {
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editName, setEditName] = useState<string>('');
    const [editPhone, setEditPhone] = useState<string>('');

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
        setEditName(customer.name);
        setEditPhone(customer.phoneNumber);
    };

    const handleSaveEdit = async (id: number) => {
        try {
            await api.put(`/admin/users/${id}`, {
                name: editName,
                phoneNumber: editPhone
            });
            setEditingId(null);
            fetchCustomers();
        } catch (err) {
            console.error('Save error', err);
        }
    };

    return (
        <CustomersContainer>
            <h1>Customers</h1>
            {customers.map(c => (
                <CustomerItem key={c.id}>
                    {editingId === c.id ? (
                        <>
                            <input
                                value={editName}
                                onChange={e => setEditName(e.target.value)}
                                placeholder="Name"
                            />
                            <br />
                            <input
                                value={editPhone}
                                onChange={e => setEditPhone(e.target.value)}
                                placeholder="Phone Number"
                            />
                            <br />
                            <Button onClick={() => handleSaveEdit(c.id)}>Save</Button>
                            <Button onClick={() => setEditingId(null)}>Cancel</Button>
                        </>
                    ) : (
                        <>
                            <h2>{c.name}</h2>
                            <p>Phone: {c.phoneNumber}</p>
                            <Button onClick={() => handleStartEdit(c)}>Edit</Button>
                            <Button onClick={() => handleDelete(c.id)}>Delete</Button>
                        </>
                    )}
                </CustomerItem>
            ))}
        </CustomersContainer>
    );
};

export default Customers;
