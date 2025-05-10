import React from 'react';
import styled from 'styled-components';

const Overlay = styled.div`
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
`;

const Dialog = styled.div`
    background: white;
    padding: 24px;
    border-radius: 10px;
    width: 300px;
    text-align: center;
    box-shadow: 0 5px 15px rgba(0,0,0,0.2);
`;

const Button = styled.button`
    margin: 8px;
    padding: 8px 14px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-weight: 500;
`;

const ConfirmButton = styled(Button)`
    background: #d63031;
    color: white;
`;

const CancelButton = styled(Button)`
    background: #b2bec3;
    color: white;
`;

interface ConfirmDialogProps {
    message: string;
    onConfirm: () => void;
    onCancel: () => void;
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({ message, onConfirm, onCancel }) => {
    return (
        <Overlay>
            <Dialog>
                <p>{message}</p>
                <div>
                    <ConfirmButton onClick={onConfirm}>Yes</ConfirmButton>
                    <CancelButton onClick={onCancel}>Cancel</CancelButton>
                </div>
            </Dialog>
        </Overlay>
    );
};

export default ConfirmDialog;