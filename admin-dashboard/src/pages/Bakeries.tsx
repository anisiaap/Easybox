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
    background-color: #0066cc;
    color: white;
    border: none;
    padding: 8px 14px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s ease;
    &:hover {
        background-color: #004a99;
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

interface Bakery {
    id: number;
    name: string;
    phone: string;
    pluginInstalled: boolean;
    token: string;
}

const Bakeries: React.FC = () => {
    const [bakeries, setBakeries] = useState<Bakery[]>([]);
    const [newBakery, setNewBakery] = useState<Omit<Bakery, 'id'>>({
        name: '',
        phone: '',
        pluginInstalled: true, // ✅ created by admin = auto approved
        token: ''
    });
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editData, setEditData] = useState<Omit<Bakery, 'id'>>({
        name: '',
        phone: '',
        pluginInstalled: false,
        token: ''
    });

    useEffect(() => {
        fetchBakeries();
    }, []);

    const fetchBakeries = async () => {
        try {
            const response = await api.get('/admin/bakeries');
            setBakeries(response.data);
        } catch (error) {
            console.error('Error fetching bakeries', error);
        }
    };

    const handleCreateBakery = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await api.post('/auth/register-bakery', newBakery);
            setNewBakery({ name: '', phone: '', pluginInstalled: true, token: '' });
            fetchBakeries();
        } catch (error) {
            console.error('Error creating bakery', error);
        }
    };

    const handleDeleteBakery = async (id: number) => {
        try {
            await api.delete(`/admin/bakeries/${id}`);
            fetchBakeries();
        } catch (error) {
            console.error('Error deleting bakery', error);
        }
    };

    const handleStartEdit = (bakery: Bakery) => {
        setEditingId(bakery.id);
        setEditData({
            name: bakery.name,
            phone: bakery.phone,
            pluginInstalled: bakery.pluginInstalled,
            token: bakery.token
        });
    };

    const handleSaveEdit = async (id: number) => {
        try {
            await api.put(`/admin/bakeries/${id}`, editData);
            setEditingId(null);
            fetchBakeries();
        } catch (error) {
            console.error('Error updating bakery', error);
        }
    };

    const handleApprove = async (id: number) => {
        try {
            await api.put(`/admin/bakeries/${id}`, { ...bakeries.find(b => b.id === id), pluginInstalled: true });
            fetchBakeries();
        } catch (error) {
            console.error('Error approving bakery', error);
        }
    };

    return (
        <Container>
            <Title>Bakeries</Title>

            {bakeries.map(b => (
                <Card key={b.id}>
                    {editingId === b.id ? (
                        <div>
                            <h3>Edit Bakery</h3>
                            <Input
                                value={editData.name}
                                onChange={e => setEditData({ ...editData, name: e.target.value })}
                                placeholder="Bakery Name"
                            />
                            <Input
                                value={editData.phone}
                                onChange={e => setEditData({ ...editData, phone: e.target.value })}
                                placeholder="Phone Number"
                            />
                            <ButtonGroup>
                                <Button onClick={() => handleSaveEdit(b.id)}>Save</Button>
                                <Button onClick={() => setEditingId(null)}>Cancel</Button>
                            </ButtonGroup>
                        </div>
                    ) : (
                        <div>
                            <h2>{b.name}</h2>
                            <Label><strong>Phone:</strong> {b.phone}</Label>
                            <Label><strong>Approved:</strong> {b.pluginInstalled ? '✅ Yes' : '❌ No'}</Label>
                            <Label><strong>Token:</strong> <code>{b.token}</code></Label>
                            <ButtonGroup>
                                <Button onClick={() => handleStartEdit(b)}>Edit</Button>
                                <Button onClick={() => handleDeleteBakery(b.id)}>Delete</Button>
                                {!b.pluginInstalled && (
                                    <Button onClick={() => handleApprove(b.id)}>Approve</Button>
                                )}
                            </ButtonGroup>
                        </div>
                    )}
                </Card>
            ))}

            <Form onSubmit={handleCreateBakery}>
                <h3>Add New Bakery (Approved)</h3>
                <Input
                    value={newBakery.name}
                    onChange={e => setNewBakery({ ...newBakery, name: e.target.value })}
                    placeholder="Bakery Name"
                />
                <Input
                    value={newBakery.phone}
                    onChange={e => setNewBakery({ ...newBakery, phone: e.target.value })}
                    placeholder="Phone Number"
                />
                <Button type="submit">Create Bakery</Button>
            </Form>
        </Container>
    );
};

export default Bakeries;
