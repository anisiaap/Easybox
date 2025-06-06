// import React, { useState } from 'react';
// import styled from 'styled-components';
// import toast from 'react-hot-toast';
// import type { EasyboxDto } from '../types/easybox'; // adjust path as needed
// const DialogContainer = styled.div`
//   padding: 20px;
//   background: white;
//   width: 400px;
//   border-radius: 8px;
// `;
//
// const Button = styled.button`
//   padding: 8px 12px;
//   margin-right: 10px;
//   cursor: pointer;
// `;
//
// const BoxList = styled.div`
//   max-height: 300px;
//   overflow-y: auto;
//   margin-top: 10px;
// `;
//
// const BoxItem = styled.div<{ selected: boolean }>`
//   padding: 8px;
//   background: ${props => (props.selected ? '#d0f0c0' : '#f8f8f8')};
//   margin-bottom: 5px;
//   cursor: pointer;
//   border-radius: 4px;
// `;
//
//
// interface Props {
//   boxes: EasyboxDto[];
//   onSelect: (box: EasyboxDto) => void | Promise<void>;
//   onClose: () => void;
// }
//
// const AdminEasyboxPickerDialog: React.FC<Props> = ({ boxes, onSelect, onClose }) => {
//   const [selectedBoxId, setSelectedBoxId] = useState<number | null>(null);
//
//   const handleConfirm = () => {
//     const selected = boxes.find(b => b.id === selectedBoxId);
//     if (selected) {
//       onSelect(selected);
//     } else {
//       toast.error("Please select a box first.");
//     }
//   };
//
//   return (
//       <DialogContainer>
//         <h2>Select Easybox</h2>
//         <BoxList>
//           {boxes.map(box => (
//               <BoxItem
//                   key={box.id}
//                   selected={box.id === selectedBoxId}
//                   onClick={() => setSelectedBoxId(box.id)}
//               >
//                 <b>{box.address}</b>
//                 <br />
//                 Status: {box.status}
//                 {box.distance !== undefined && <div>Distance: {box.distance.toFixed(1)} km</div>}
//                 {box.recommended && <div>(Recommended)</div>}
//               </BoxItem>
//           ))}
//         </BoxList>
//
//         <div style={{ marginTop: 15 }}>
//           <Button onClick={handleConfirm} disabled={!selectedBoxId}>Confirm</Button>
//           <Button onClick={onClose}>Cancel</Button>
//         </div>
//       </DialogContainer>
//   );
// };
//
// export default AdminEasyboxPickerDialog;