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
export const SIZE_MAP: Record<number, string> = {
    10: 'Small',
    15: 'Medium',
    20: 'Large',
};

export const TEMPERATURE_MAP: Record<number, string> = {
    4: 'Refrigerated',
    8: 'Cool',
    12: 'Room Temperature',
};

export const STATUS_MAP: Record<string, string> = {
    free: 'Available for use',
    busy: 'Occupied or reserved',
};

export const CONDITION_MAP: Record<string, string> = {
    good: 'Fully operational',
    dirty: 'Needs cleaning',
    broken: 'Not functioning',
};

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
    padding: 10px;
    border-radius: 6px;
    border: 1px solid #ccc;
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

const pageSize = 10;

const AdminCompartments: React.FC = () => {
    const [compartments, setCompartments] = useState<Compartment[]>([]);
    const [filtered, setFiltered] = useState<Compartment[]>([]);
    const [searchAddress, setSearchAddress] = useState('');
    const [searchStatus, setSearchStatus] = useState('');
    const [searchCondition, setSearchCondition] = useState('');
    const [confirmDialog, setConfirmDialog] = useState<ConfirmAction>(null);
    const [page, setPage] = useState(0);

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
        const address = searchAddress.trim().toLowerCase();
        const status = searchStatus.trim().toLowerCase();
        const condition = searchCondition.trim().toLowerCase();

        const result = compartments.filter(c =>
            c.easyboxAddress.toLowerCase().includes(address) &&
            c.status.toLowerCase().includes(status) &&
            c.condition.toLowerCase().includes(condition)
        );
        setFiltered(result);
        setPage(0);
    }, [searchAddress, searchStatus, searchCondition, compartments]);

    const pagedData = filtered.slice(page * pageSize, (page + 1) * pageSize);

    return (
        <Container>
            <Title>Compartments Overview</Title>

            <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <Input
                    placeholder="Search Easybox Address"
                    value={searchAddress}
                    onChange={(e) => setSearchAddress(e.target.value)}
                />
                <Input
                    placeholder="Search Status"
                    value={searchStatus}
                    onChange={(e) => setSearchStatus(e.target.value)}
                />
                <Input
                    placeholder="Search Condition"
                    value={searchCondition}
                    onChange={(e) => setSearchCondition(e.target.value)}
                />
                <Button onClick={fetchCompartments}>Refresh</Button>
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
                    {pagedData.map(c => (
                        <TableRow key={c.id}>
                            <Td>{c.id}</Td>
                            <Td>{STATUS_MAP[c.status] || c.status}</Td>
                            <Td>{CONDITION_MAP[c.condition] || c.condition}</Td>
                            <Td>{SIZE_MAP[c.size] || c.size}</Td>
                            <Td>{TEMPERATURE_MAP[c.temperature] || c.temperature}</Td>
                            <Td>{c.easyboxAddress}</Td>
                            {/*<Td>{c.status}</Td>*/}
                            {/*<Td>{c.condition}</Td>*/}
                            {/*<Td>{c.size}</Td>*/}
                            {/*<Td>{c.temperature}</Td>*/}
                            {/*<Td>{c.easyboxAddress}</Td>*/}
                        </TableRow>
                    ))}
                    </tbody>
                </Table>

                <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginBottom: '20px' }}>
                    <Button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</Button>
                    <span>Page {page + 1} of {Math.ceil(filtered.length / pageSize)}</span>
                    <Button
                        onClick={() => setPage(p => (p + 1 < Math.ceil(filtered.length / pageSize) ? p + 1 : p))}
                        disabled={(page + 1) >= Math.ceil(filtered.length / pageSize)}
                    >
                        Next
                    </Button>
                </div>
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
