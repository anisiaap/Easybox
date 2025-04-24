import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import axios from 'axios';
import L from 'leaflet';

// Styled Components
const DashboardContainer = styled.div`
    display: flex;
    flex-direction: row;
    height: 100vh;
`;

const MapWrapper = styled.div`
    flex: 2;
    position: relative;
`;

const DetailsPanel = styled.div`
    flex: 1;
    padding: 20px;
    background: #f8f8f8;
    overflow-y: auto;
`;

const SearchBar = styled.input`
    width: 100%;
    padding: 10px;
    margin-bottom: 20px;
    border: 1px solid #ccc;
    border-radius: 4px;
`;

const EasyboxItem = styled.div`
    padding: 10px;
    border-bottom: 1px solid #ddd;
    cursor: pointer;
    &:hover {
        background-color: #eaeaea;
    }
`;

const BackButton = styled.button`
    padding: 8px 16px;
    background-color: #104208;
    border: none;
    color: white;
    border-radius: 4px;
    margin-bottom: 20px;
    cursor: pointer;
    &:hover {
        background-color: #0d3318;
    }
`;

const CompartmentsGrid = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    margin-top: 20px;
`;

// Helper functions for dynamic styling based on compartment properties.
const getBorderColor = (status: string) => {
    switch (status.toLowerCase()) {
        case "free":
            return "#28a745"; // green
        case "busy":
            return "#dc3545"; // red
        default:
            return "#ddd";
    }
};

const getBackgroundColor = (condition: string) => {
    switch (condition.toLowerCase()) {
        case "good":
            return "#e9f7ef"; // light green
        case "dirty":
            return "#fff3cd"; // light yellow
        case "broken":
            return "#f8d7da"; // light red
        default:
            return "white";
    }
};

const CompartmentCard = styled.div<{ status: string; condition: string }>`
    padding: 12px;
    border: 2px solid ${props => getBorderColor(props.status)};
    border-radius: 8px;
    background: ${props => getBackgroundColor(props.condition)};
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    text-align: left;
    transition: transform 0.2s;
    &:hover {
        transform: scale(1.02);
    }
    h4 {
        margin: 0 0 8px 0;
        font-size: 1.1em;
        color: #333;
    }
    p {
        margin: 4px 0;
        font-size: 0.9em;
        color: #555;
    }`
;

const customIcon = new L.Icon({
    iconUrl: '/pin.png',
    iconSize: [40, 50],
    iconAnchor: [20, 50],
    popupAnchor: [0, -45],
});

// Interfaces
interface Easybox {
    id: number;
    address: string;
    latitude: number;
    longitude: number;
    status: string;
    deviceUrl?: string;
}

interface Compartment {
    id: number;
    size: number;
    temperature: number;
    status: string;
    condition: string;
}

interface PredefinedValue {
    value: string;
    description: string;
}

interface PredefinedValues {
    temperature: PredefinedValue[];
    size: PredefinedValue[];
    status: PredefinedValue[];
    condition: PredefinedValue[];
}

// Aggregated DTO returned by the central server
interface DeviceDetails extends Easybox {
    compartments: Compartment[];
    predefinedValues: PredefinedValues;
}

// Custom component to set map view
const SetView: React.FC<{ center: [number, number]; zoom: number }> = ({ center, zoom }) => {
    const map = useMap();
    useEffect(() => {
        map.setView(center, zoom);
    }, [map, center, zoom]);
    return null;
};

// Helper function to get description (if needed in UI)
const getDescription = (predefinedArray: PredefinedValue[] | undefined, value: number | string): string => {
    if (!predefinedArray) return String(value);
    const found = predefinedArray.find(item => item.value === String(value));
    return found ? found.description : String(value);
};

const Dashboard: React.FC = () => {
    const [easyboxes, setEasyboxes] = useState<Easybox[]>([]);
    const [filteredEasyboxes, setFilteredEasyboxes] = useState<Easybox[]>([]);
    const [search, setSearch] = useState<string>('');
    const [selectedEasybox, setSelectedEasybox] = useState<DeviceDetails | null>(null);

    const center: [number, number] = [45.9432, 24.9668]; // Center in Romania
    const zoom = 6;
    const tileLayerProps = {
        attribution: '&copy; OpenStreetMap contributors',
        url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
    };

    // Fetch easyboxes from the backend
    useEffect(() => {
        const fetchEasyboxes = async () => {
            try {
                const response = await axios.get('http://localhost:8080/api/admin/easyboxes');
                setEasyboxes(response.data);
                setFilteredEasyboxes(response.data);
            } catch (error) {
                console.error('Error fetching easyboxes:', error);
            }
        };
        fetchEasyboxes();
    }, []);

    // Filter easyboxes based on search term
    useEffect(() => {
        const filtered = easyboxes.filter(box =>
            box.address.toLowerCase().includes(search.toLowerCase())
        );
        setFilteredEasyboxes(filtered);
    }, [search, easyboxes]);

    const handleSelectEasybox = async (box: Easybox) => {
        if (!box.deviceUrl) {
            alert("Device URL not available.");
            return;
        }
        try {
            // Call the central server aggregated endpoint.
            const response = await axios.get(`http://localhost:8080/api/admin/easyboxes/${box.id}/details`);
    // Merge the box with the returned aggregated details.
    const details: DeviceDetails = { ...box, ...response.data };
setSelectedEasybox(details);
} catch (error) {
    console.error('Error fetching device details:', error);
    alert('Failed to fetch compartment details.');
}
};

return (
    <DashboardContainer>
        <MapWrapper>
            <MapContainer
                style={{ width: '100%', height: '100%' }}
                {...({ initialViewState: { center, zoom } } as any)}
            >
                <SetView center={center} zoom={zoom} />
                <TileLayer {...tileLayerProps} />
                {filteredEasyboxes.map(box => (
                    <Marker
                        key={box.id}
                        position={[box.latitude, box.longitude]}
                        icon={customIcon}
                        eventHandlers={{
                            click: () => handleSelectEasybox(box)
                        }}
                    >
                        <Popup>
                            <div>
                                <strong>{box.address}</strong>
                                <br />
                                Status: {box.status}
                                <br />
                                <button onClick={() => handleSelectEasybox(box)}>Details</button>
                            </div>
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </MapWrapper>
        <DetailsPanel>
            <SearchBar
                type="text"
                placeholder="Search Easyboxes..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />
            {selectedEasybox ? (
                <div>
                    <BackButton onClick={() => setSelectedEasybox(null)}>
                        &larr; Back to List
                    </BackButton>
                    <h2>{selectedEasybox.address}</h2>
                    <p>Status: {selectedEasybox.status}</p>
                    <h3>Compartments</h3>
                    {selectedEasybox.compartments.length > 0 ? (
                        <CompartmentsGrid>
                            {selectedEasybox.compartments.map(comp => (
                                <CompartmentCard
                                    key={comp.id}
                                    status={comp.status}
                                    condition={comp.condition}
                                >
                                    <h4>Compartment {comp.id}</h4>
                                    <p><strong>Size:</strong> {getDescription(selectedEasybox.predefinedValues?.size, comp.size)}</p>
                                    <p><strong>Temperature:</strong> {getDescription(selectedEasybox.predefinedValues?.temperature, comp.temperature)}°C</p>
                                    <p><strong>Status:</strong> {getDescription(selectedEasybox.predefinedValues?.status, comp.status)}</p>
                                    <p><strong>Condition:</strong> {getDescription(selectedEasybox.predefinedValues?.condition, comp.condition)}</p>
                                </CompartmentCard>
                            ))}
                        </CompartmentsGrid>
                    ) : (
                        <p>No compartments found.</p>
                    )}
                </div>
            ) : (
                <div>
                    <h2>Select an Easybox on the map</h2>
                    <p>Click on a marker or a list item to view details.</p>
                    {filteredEasyboxes.map(box => (
                        <EasyboxItem key={box.id} onClick={() => handleSelectEasybox(box)}>
                            <strong>{box.address}</strong> — Status: {box.status}
                        </EasyboxItem>
                    ))}
                </div>
            )}
        </DetailsPanel>
    </DashboardContainer>
);
};

export default Dashboard;
