// src/pages/Bakeries.tsx
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { api } from '../api'; // make sure you have axios api client configured!

// Styled Components
const BakeriesContainer = styled.div`
    padding: 20px;
`;
const BakeryCard = styled.div`
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

// Interfaces
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
        pluginInstalled: false,
        token: ''
    });
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editData, setEditData] = useState<Omit<Bakery, 'id'>>({
        name: '',
        phone: '',
        pluginInstalled: false,
        token: ''
    });

    // Load all bakeries
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
            await api.post('/admin/bakeries', newBakery);
            setNewBakery({ name: '', phone: '', pluginInstalled: false, token: '' });
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
        setEditData({ name: bakery.name, phone: bakery.phone, pluginInstalled: bakery.pluginInstalled, token: bakery.token });
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

    return (
        <BakeriesContainer>
            <h1>Bakeries</h1>

            {/* List of bakeries */}
            {bakeries.map(b => (
                <BakeryCard key={b.id}>
                    {editingId === b.id ? (
                        <div>
                            <input
                                value={editData.name}
                                onChange={e => setEditData({...editData, name: e.target.value})}
                                placeholder="Name"
                            /><br/>
                            <input
                                value={editData.phone}
                                onChange={e => setEditData({...editData, phone: e.target.value})}
                                placeholder="Phone"
                            /><br/>
                            <label>
                                Plugin Installed:
                                <input
                                    type="checkbox"
                                    checked={editData.pluginInstalled}
                                    onChange={e => setEditData({...editData, pluginInstalled: e.target.checked})}
                                />
                            </label><br/>
                            <input
                                value={editData.token}
                                onChange={e => setEditData({...editData, token: e.target.value})}
                                placeholder="Token"
                            /><br/>
                            <Button onClick={() => handleSaveEdit(b.id)}>Save</Button>
                            <Button onClick={() => setEditingId(null)}>Cancel</Button>
                        </div>
                    ) : (
                        <div>
                            <h2>{b.name}</h2>
                            <p><strong>Phone:</strong> {b.phone}</p>
                            <p><strong>Plugin Installed:</strong> {b.pluginInstalled ? 'Yes' : 'No'}</p>
                            <p><strong>Token:</strong> {b.token}</p>
                            <Button onClick={() => handleStartEdit(b)}>Edit</Button>
                            <Button onClick={() => handleDeleteBakery(b.id)}>Delete</Button>
                        </div>
                    )}
                </BakeryCard>
            ))}

            {/* Create new bakery */}
            <Form onSubmit={handleCreateBakery}>
                <h3>Add New Bakery</h3>
                <input
                    value={newBakery.name}
                    onChange={e => setNewBakery({...newBakery, name: e.target.value})}
                    placeholder="Name"
                /><br/>
                <input
                    value={newBakery.phone}
                    onChange={e => setNewBakery({...newBakery, phone: e.target.value})}
                    placeholder="Phone"
                /><br/>
                <label>
                    Plugin Installed:
                    <input
                        type="checkbox"
                        checked={newBakery.pluginInstalled}
                        onChange={e => setNewBakery({...newBakery, pluginInstalled: e.target.checked})}
                    />
                </label><br/>
                <input
                    value={newBakery.token}
                    onChange={e => setNewBakery({...newBakery, token: e.target.value})}
                    placeholder="Token"
                /><br/>
                <Button type="submit">Create</Button>
            </Form>
        </BakeriesContainer>
    );
};

export default Bakeries;
