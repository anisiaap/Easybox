import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { api } from '../api';

const Container = styled.div`
    padding: 32px;
    max-width: 800px;
    margin: 0 auto;
`;
const TableWrapper = styled.div`
    width: 100%;
    overflow-x: auto;
    margin-bottom: 32px;
`;


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



const TableRow = styled.tr`
    &:hover {
        background-color: #f0f8f0;
    }
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

const Form = styled.form`
    margin-top: 40px;
    background: #f9f9f9;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
`;
const Td = styled.td`
    padding: 16px;
    border-bottom: 1px solid #eee;
    vertical-align: top;
    word-break: break-word;
    max-width: 250px;
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
    const [copiedTokenId, setCopiedTokenId] = useState<number | null>(null);
    const [newBakery, setNewBakery] = useState<Omit<Bakery, 'id'>>({
        name: '',
        phone: '',
        pluginInstalled: true,
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
            api.post('/auth/register-bakery', newBakery);
            setNewBakery({ name: '', phone: '', pluginInstalled: true, token: '' });
            fetchBakeries();
        } catch (error) {
            console.error('Error creating bakery', error);
        }
    };

    const handleDeleteBakery = async (id: number) => {
        const confirmDelete = window.confirm('Are you sure you want to delete this bakery? This action cannot be undone.');
        if (!confirmDelete) return;

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
        const confirmSave = window.confirm('Are you sure you want to save the changes to this bakery?');
        if (!confirmSave) return;
        try {
            await api.put(`/admin/bakeries/${id}`, editData);
            setEditingId(null);
            fetchBakeries();
        } catch (error) {
            console.error('Error updating bakery', error);
        }
    };

    const handleApprove = async (id: number) => {
        const confirmSave = window.confirm('Are you sure you want to approve this order bakery?');
        if (!confirmSave) return;
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
            <TableWrapper>
            <Table>
                <thead>
                <tr>
                    <Th>Name</Th>
                    <Th>Phone</Th>
                    <Th>Approved</Th>
                    <Th>Token</Th>
                    <Th>Actions</Th>
                </tr>
                </thead>
                <tbody>
                {bakeries.map(b => (
                    <TableRow key={b.id}>
                        <Td>
                            {editingId === b.id ? (
                                <Input
                                    value={editData.name}
                                    onChange={e =>
                                        setEditData({ ...editData, name: e.target.value })
                                    }
                                    placeholder="Bakery Name"
                                />
                            ) : (
                                b.name
                            )}
                        </Td>
                        <Td>
                            {editingId === b.id ? (
                                <Input
                                    value={editData.phone}
                                    onChange={e =>
                                        setEditData({ ...editData, phone: e.target.value })
                                    }
                                    placeholder="Phone Number"
                                />
                            ) : (
                                b.phone
                            )}
                        </Td>
                        <Td>{b.pluginInstalled ? '✅' : '❌'}</Td>
                        <Td>
                            <Button onClick={() => {
                                navigator.clipboard.writeText(b.token);
                                setCopiedTokenId(b.id);
                                setTimeout(() => setCopiedTokenId(null), 2000);
                            }}>
                                📋 Copy
                            </Button>
                            {copiedTokenId === b.id && <span style={{ marginLeft: 8, color: 'green' }}>✅ Copied!</span>}
                        </Td>
                        <Td>
                            <ButtonGroup>
                                {editingId === b.id ? (
                                    <>
                                        <Button onClick={() => handleSaveEdit(b.id)}>Save</Button>
                                        <Button onClick={() => setEditingId(null)}>Cancel</Button>
                                    </>
                                ) : (
                                    <>
                                        <Button onClick={() => handleStartEdit(b)}>Edit</Button>
                                        <Button onClick={() => handleDeleteBakery(b.id)}>Delete</Button>
                                        {!b.pluginInstalled && (
                                            <Button onClick={() => handleApprove(b.id)}>Approve</Button>
                                        )}
                                    </>
                                )}
                            </ButtonGroup>
                        </Td>
                        {/*<Td>*/}
                        {/*    <code style={{ display: 'inline-block', maxWidth: 200, overflowX: 'auto', whiteSpace: 'nowrap' }}>*/}
                        {/*        {b.token}*/}
                        {/*    </code>*/}
                        {/*</Td>*/}

                    </TableRow>
                ))}
                </tbody>
            </Table>

                </TableWrapper>
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
