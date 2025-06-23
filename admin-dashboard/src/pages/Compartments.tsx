import React, { useEffect, useState, useCallback } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import { api } from '../api';
import ConfirmDialog from '../components/ui/ConfirmDialog';

const Container = styled.div`
    padding: 32px;
    max-width: 1000px;
    margin: 0 auto;
`;

const Title = styled.h1`
    font-size: 28px;
    margin-bottom: 24px;
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

const Td = styled.td`
    padding: 16px;
    border-bottom: 1px solid #eee;
    vertical-align: top;
    word-break: break-word;
    max-width: 250px;
`;

const TableRow = styled.tr`
    &:hover {
        background-color: #f0f8f0;
    }
`;

const Input = styled.input`
    width: 100%;
    margin-bottom: 12px;
    padding: 10px;
    border-radius: 6px;
    border: 1px solid #ccc;
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

interface Compartment {
    id: number;
    status: string;
    condition: string;
    size: number;
    temperature: number;
    easyboxAddress: string;
}

type ConfirmAction = null | {
    message: string;
    onConfirm: () => void;
};

const AdminCompartments: React.FC = () => {
    const [compartments, setCompartments] = useState<Compartment[]>([]);
    const [filtered, setFiltered] = useState<Compartment[]>([]);
    const [searchAddress, setSearchAddress] = useState('');
    const [searchStatus, setSearchStatus] = useState('');
    const [confirmDialog, setConfirmDialog] = useState<ConfirmAction>(null);

    const fetchCompartments = useCallback(async () => {
        try {
            const res = await api.get('/admin/compartments');
            setCompartments(res.data);
            setFiltered(res.data);
        } catch (error: any) {
            toast.error(error?.response?.data?.message || 'Something went wrong');
        }
    }, []);

    useEffect(() => {
        fetchCompartments();
    }, [fetchCompartments]);

    useEffect(() => {
        const lowerAddress = searchAddress.trim().toLowerCase();
        const lowerStatus = searchStatus.trim().toLowerCase();

        setFiltered(
            compartments.filter(c =>
                c.easyboxAddress.toLowerCase().includes(lowerAddress) &&
                c.status.toLowerCase().includes(lowerStatus)
            )
        );
    }, [searchAddress, searchStatus, compartments]);

    return (
        <Container>
            <Title>Compartments Overview</Title>

            {/* Search inputs */}
            <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <Input
                    placeholder="Search by Easybox address"
                    value={searchAddress}
                    onChange={(e) => setSearchAddress(e.target.value)}
                />
                <Input
                    placeholder="Search by status (e.g. free, busy)"
                    value={searchStatus}
                    onChange={(e) => setSearchStatus(e.target.value)}
                />
                <Button onClick={fetchCompartments}>Search</Button>
            </div>

            <TableWrapper>
                <Table>
                    <thead>
                    <tr>
                        <Th>ID</Th>
                        <Th>Status</Th>
                        <Th>Condition</Th>
                        <Th>Size</Th>
                        <Th>Temperature (°C)</Th>
                        <Th>Easybox Address</Th>
                    </tr>
                    </thead>
                    <tbody>
                    {filtered.map(c => (
                        <TableRow key={c.id}>
                            <Td>{c.id}</Td>
                            <Td>{c.status}</Td>
                            <Td>{c.condition}</Td>
                            <Td>{c.size}</Td>
                            <Td>{c.temperature}</Td>
                            <Td>
                                {c.easyboxAddress}
                            </Td>
                        </TableRow>
                    ))}
                    </tbody>
                </Table>
            </TableWrapper>

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

export default AdminCompartments;
