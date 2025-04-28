import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { api } from '../api'; // your api instance

// Styled
const DialogContainer = styled.div`
  padding: 20px;
  background: white;
  width: 400px;
  border-radius: 8px;
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 8px;
  margin-bottom: 10px;
`;

const Button = styled.button`
  padding: 8px 12px;
  margin-right: 10px;
  cursor: pointer;
`;

const BoxList = styled.div`
  max-height: 300px;
  overflow-y: auto;
  margin-top: 10px;
`;

const BoxItem = styled.div<{ selected: boolean }>`
  padding: 8px;
  background: ${props => (props.selected ? '#d0f0c0' : '#f8f8f8')};
  margin-bottom: 5px;
  cursor: pointer;
  border-radius: 4px;
`;

interface Easybox {
  id: number;
  address: string;
  status: string;
  latitude: number;
  longitude: number;
  recommended?: boolean;
}

interface Props {
  onSelect: (easybox: Easybox) => void;
  onClose: () => void;
}

const AdminEasyboxPickerDialog: React.FC<Props> = ({ onSelect, onClose }) => {
  const [address, setAddress] = useState('');
  const [easyboxes, setEasyboxes] = useState<Easybox[]>([]);
  const [selectedBoxId, setSelectedBoxId] = useState<number | null>(null);

  const handleSearch = async () => {
    try {
      const body = {
        address,
        minTemperature: 5,
        totalDimension: 10,
      };
      const res = await api.post('/widget/reservation/available', body);
      const data = res.data;

      const merged: Easybox[] = [];
      if (data.recommendedBox) {
        merged.push({ ...data.recommendedBox, recommended: true });
      }
      if (data.otherBoxes) {
        merged.push(...data.otherBoxes);
      }
      setEasyboxes(merged);
    } catch (error) {
      console.error('Search failed:', error);
    }
  };

  const handleConfirm = () => {
    const selected = easyboxes.find(b => b.id === selectedBoxId);
    if (selected) {
      onSelect(selected);
    }
  };

  return (
    <DialogContainer>
      <h2>Select Easybox</h2>
      <SearchInput
        type="text"
        placeholder="Enter address"
        value={address}
        onChange={e => setAddress(e.target.value)}
      />
      <Button onClick={handleSearch}>Check Availability</Button>

      <BoxList>
        {easyboxes.map(box => (
          <BoxItem
            key={box.id}
            selected={box.id === selectedBoxId}
            onClick={() => setSelectedBoxId(box.id)}
          >
            <b>{box.address}</b>
            <br />
            Status: {box.status}
            {box.recommended && <div>(Recommended)</div>}
          </BoxItem>
        ))}
      </BoxList>

      <div style={{ marginTop: 15 }}>
        <Button onClick={handleConfirm} disabled={!selectedBoxId}>Confirm</Button>
        <Button onClick={onClose}>Cancel</Button>
      </div>
    </DialogContainer>
  );
};

export default AdminEasyboxPickerDialog;
