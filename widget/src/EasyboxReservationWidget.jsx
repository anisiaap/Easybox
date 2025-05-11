import React, {
    useEffect,
    useState,
    useRef,
    forwardRef,
    useImperativeHandle
} from 'react';

import { api } from './api';
import axios from 'axios';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

import iconUrl from 'leaflet/dist/images/marker-icon.png';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';
import { setWidgetJwt } from './api';
L.Marker.prototype.options.icon = L.icon({
    iconRetinaUrl,
    iconUrl,
    shadowUrl,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});

const EasyboxReservationWidget = forwardRef(
    (
        {
            apiUrl,
            clientAddress = '',
            additionalCriteria = {},
            onReservationSelect
        },
        ref
    ) => {
        // -------------------
        // State & Refs
        // -------------------
        const mapRef = useRef(null);
        const [leafletMap, setLeafletMap] = useState(null);
        const [markersLayer, setMarkersLayer] = useState(null);

        // The user can type a new address to re-center the map
        const [searchAddress, setSearchAddress] = useState(clientAddress);
        const [searchInput, setSearchInput] = useState(clientAddress);
        const [jwtToken, setJwtToken] = useState(null);


        // We store the resulting lat/lng of the client’s address (or typed address)
        const [mapCenter, setMapCenter] = useState([45.9432, 24.9668]); // fallback: center of Romania
        const [mapZoom, setMapZoom] = useState(7);

        // List of boxes returned by /reservations/available
        // We'll merge recommendedBox + otherBoxes into a single array
        const [easyboxes, setEasyboxes] = useState([]);
        // The user’s current selection
        const [selectedBox, setSelectedBox] = useState(null);
        const [loading, setLoading] = useState(false);
        // Imperative handle so parent can call "invalidateMapSize()" if needed
        useImperativeHandle(ref, () => ({
            invalidateMapSize() {
                if (leafletMap) {
                    leafletMap.invalidateSize();
                }
            }
        }));

        // -------------------
        // Map Initialization
        // -------------------
        useEffect(() => {
            if (leafletMap) return; // If we already have an instance, skip

            // Create the map
            const mapInstance = L.map(mapRef.current, {
                center: mapCenter,
                zoom: mapZoom,
                zoomControl: true
            });

            // Add a tile layer
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors'
            }).addTo(mapInstance);

            // Create a layer for the markers
            const layerGroup = L.layerGroup().addTo(mapInstance);

            setLeafletMap(mapInstance);
            setMarkersLayer(layerGroup);
        }, [leafletMap, mapCenter, mapZoom]);
        useEffect(() => {
            function handleJwtMessage(event) {
                if (event.data?.type === "init-jwt") {
                    setJwtToken(event.data.token);
                    setWidgetJwt(event.data.token);
                    console.log("🔐 Received JWT:", event.data.token);
                }
            }
            window.addEventListener("message", handleJwtMessage);
            return () => window.removeEventListener("message", handleJwtMessage);
        }, []);

        // Whenever the user changes the address, or the widget loads,
        // we attempt to geocode the new address and center the map on it
        useEffect(() => {
            if (!searchAddress) return;

            const geocodeAddress = async (addr) => {
                try {
                    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
                        addr
                    )}`;
                    const response = await axios.get(url);
                    if (response.data && response.data.length > 0) {
                        // Take the first result
                        const { lat, lon } = response.data[0];
                        setMapCenter([parseFloat(lat), parseFloat(lon)]);
                        setMapZoom(13);
                        // If the map is already created, fly there
                        if (leafletMap) {
                            leafletMap.flyTo([parseFloat(lat), parseFloat(lon)], 13, {
                                duration: 1.0
                            });
                        }
                    }
                } catch (err) {
                    console.error('Geocoding error:', err);
                }
            };

            geocodeAddress(searchAddress);
        }, [searchAddress, leafletMap]);


        // -------------------
        // Updating Markers on the Map
        // -------------------
        useEffect(() => {
            if (!markersLayer) return;
            // Clear previous markers
            markersLayer.clearLayers();

            // Add a marker for each easybox
            easyboxes.forEach((box) => {
                const marker = L.marker([box.latitude, box.longitude]);
                let popupContent = `<b>${box.address}</b><br/>`;
                popupContent += `Status: ${box.status || 'Unknown'}`;
                if (box.recommended) {
                    popupContent += '<br/><i>Recommended Box</i>';
                }
                marker.bindPopup(popupContent);

                // When user clicks the marker, highlight that box
                marker.on('click', () => {
                    setSelectedBox(box);
                });
                markersLayer.addLayer(marker);
            });
        }, [markersLayer, easyboxes]);

        // -------------------
        // Compute reservation window
        // (Delivery time ± 3h before and +24h +3h after)
        // -------------------
        const computeReservationWindow = (deliveryTime) => {
            const deliveryDate = new Date(deliveryTime);
            const start = new Date(deliveryDate.getTime() - 3 * 60 * 60 * 1000); // 8h before
            const end = new Date(deliveryDate.getTime() + 27 * 60 * 60 * 1000); // 16h + 3h after
            return { start, end };
        };

        // -------------------
        // Handlers
        // -------------------
        const handleCheckAvailability = async (address = clientAddress) => {
            setLoading(true);
            try {
                const { deliveryTime, minTemperature, totalDimension } = additionalCriteria;

                let start = null;
                let end   = null;
                if (deliveryTime) {
                    const window = computeReservationWindow(deliveryTime);
                    start = window.start.toISOString().split('.')[0];
                    end   = window.end  .toISOString().split('.')[0];
                }

                const body = {
                    address,                              // 👈  use the argument
                    minTemperature : parseInt(minTemperature),
                    totalDimension : parseInt(totalDimension),
                    start,
                    end
                };

                const res  = await api.post('widget/reservation/available', body, {
                    headers: jwtToken ? { Authorization: `Bearer ${jwtToken}` } : {}
                });
                const data = res.data;

                const merged = [];
                if (data.recommendedBox) {
                    merged.push({ ...data.recommendedBox, recommended: true });
                }
                if (Array.isArray(data.otherBoxes)) merged.push(...data.otherBoxes);

                setEasyboxes(merged);
                setSelectedBox(null);
            }finally {
                setLoading(false);
            }
        };

        const handleReserve = async () => {
            if (!selectedBox) { toast.warn('Please select an Easybox'); return;  }

            const { clientName, phone, deliveryTime, minTemperature, totalDimension } = additionalCriteria;
            const { start, end } = computeReservationWindow(deliveryTime);
            const params = new URLSearchParams(window.location.search);
            const payload = {
                client       : clientName,
                phone,
                deliveryTime ,
                easyboxId    : selectedBox.id,
                minTemperature,
                totalDimension,
                reservationStart : start.toISOString().slice(0,-1),
                reservationEnd   : end.toISOString().slice(0,-1),
                bakeryId: params.get("bakeryId")
            };

            const res = await api.post(`widget/reservation/hold`, payload, {
                headers: jwtToken ? { Authorization: `Bearer ${jwtToken}` } : {}
            });

            window.parent.postMessage({
                type: "easybox-reserved",
                data: { ...res.data, address: selectedBox.address }
            }, "*");
        };

        useEffect(() => {
            function handleMsg(evt) {
                if (evt.data?.type === "bakery-order-confirmed") {
                    const id = evt.data.reservationId;
                    api.patch(`widget/reservation/${id}/confirm`, null, {
                        headers: jwtToken ? { Authorization: `Bearer ${jwtToken}` } : {}
                    })
                        .then(res => {
                            console.log("✅ Easybox confirmed!", res.data);
                            toast.success("Reservation confirmed successfully!", { autoClose: 3000 });

                            // Optionally, you can notify the parent
                            window.parent.postMessage({
                                type: "easybox-reservation-confirmed",
                                reservationId: id
                            }, "*");
                        })
                        .catch(err => {
                            console.error("❌ Confirm error:", err);
                            toast.error("Failed to confirm reservation.", { autoClose: 3000 });
                        });
                }
            }
            window.addEventListener("message", handleMsg);
            return () => window.removeEventListener("message", handleMsg);
        }, []);
        const handleSearchSubmit = (e) => {
            e.preventDefault();

            const addr = searchInput.trim();
            if (!addr) return;

            setSearchAddress(addr);      // triggers geocoding / fly‑to
            handleCheckAvailability(addr); // 👈 immediately fetch boxes
        };

        // Let the user click on a box in the side panel to focus it on the map
        const flyToBox = (box) => {
            setSelectedBox(box);
            if (leafletMap) {
                leafletMap.flyTo([box.latitude, box.longitude], 15, { duration: 1.0 });
            }
        };
        // ⬇️ runs exactly once after the component mounts
        useEffect(() => {
            // only if you actually received a starter address
            if (searchAddress?.trim()) {
                handleCheckAvailability(searchAddress.trim());
            }
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, []);            // 👈 empty deps => run once
        // -------------------
        // Rendering
        // -------------------
        return (
            <>
                <div style={styles.widgetContainer}>
                    {/* Header */}
                    <div style={styles.header}>
                        <h2 style={styles.title}>Easybox Reservation</h2>
                    </div>

                    {/* Main Content: Search + Availability + Map */}
                    <div style={styles.mainSection}>
                        {/* Left side: Searching & listing */}
                        <div style={styles.sidePanel}>
                            {/* Address search form */}
                            <form onSubmit={handleSearchSubmit} style={styles.searchForm}>
                                <input
                                    type="text"
                                    placeholder="Search or update address..."
                                    value={searchInput}
                                    onChange={(e) => setSearchInput(e.target.value)}
                                    style={styles.input}
                                />
                                <button type="submit" style={styles.button}>
                                    Search
                                </button>
                            </form>

                            {/* Availability button */}
                            {/*<button onClick={handleCheckAvailability} style={styles.checkBtn}>*/}
                            {/*    Check Available Easyboxes*/}
                            {/*</button>*/}

                            {/* List of easyboxes */}
                            <div style={styles.easyboxList}>
                                {easyboxes.length === 0 && (
                                    <div style={styles.noBoxes}>No easyboxes found yet.</div>
                                )}
                                {easyboxes.map((box) => (
                                    <div
                                        key={box.id}
                                        style={{
                                            ...styles.boxItem,
                                            backgroundColor:
                                                selectedBox && selectedBox.id === box.id
                                                    ? '#e1f5fe'
                                                    : '#fff'
                                        }}
                                        onClick={() => flyToBox(box)}
                                    >
                                        <div>
                                            <b>
                                                {box.address}
                                                {box.recommended ? ' (Recommended)' : ''}
                                            </b>
                                            <br />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Right side: The Map */}
                        <div style={styles.mapPanel}>
                            <div ref={mapRef} style={styles.map} />
                        </div>
                    </div>

                    {/* Footer / “Reserve” action */}
                    <div style={styles.footer}>
                        {selectedBox && (
                            <div style={styles.selectedInfo}>
                                <span>Selected Box: {selectedBox.address}</span>
                                {selectedBox.recommended && <span> (Recommended)</span>}
                            </div>
                        )}
                        <button
                            onClick={handleReserve}
                            style={styles.reserveBtn}
                            disabled={loading || !selectedBox}  // ← prevents clicks while data is coming
                        >
                            {loading ? 'Loading…' : 'Reserve Selected Box'}
                        </button>
                    </div>
                </div>
                <ToastContainer position="bottom-right" newestOnTop />
            </>
        );
    }
);

// --------------
// Some minimal inline styles
// --------------
const styles = {
    widgetContainer: {
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        fontFamily: 'Arial, sans-serif',
        border: '1px solid #ccc',
        borderRadius: '8px',
        overflow: 'hidden',
        backgroundColor: '#fff'
    },
    header: {
        backgroundColor: '#f7f7f7',
        padding: '10px 15px',
        borderBottom: '1px solid #ccc'
    },
    title: {
        margin: 0,
        fontSize: '1.2rem'
    },
    mainSection: {
        display: 'flex',
        flex: 1,
        minHeight: '400px'
    },
    sidePanel: {
        width: '250px',
        borderRight: '1px solid #ccc',
        padding: '10px',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column'
    },
    searchForm: {
        display: 'flex',
        marginBottom: '10px'
    },
    input: {
        flex: 1,
        padding: '6px',
        marginRight: '5px',
        border: '1px solid #ccc',
        borderRadius: '4px'
    },
    button: {
        padding: '6px 12px',
        borderRadius: '4px',
        border: '1px solid #ccc',
        cursor: 'pointer'
    },
    checkBtn: {
        padding: '6px 12px',
        marginBottom: '10px',
        borderRadius: '4px',
        border: '1px solid #ccc',
        cursor: 'pointer'
    },
    easyboxList: {
        flex: 1,
        overflowY: 'auto',
        border: '1px solid #eee',
        borderRadius: '4px',
        padding: '5px'
    },
    boxItem: {
        borderBottom: '1px solid #eee',
        padding: '8px',
        cursor: 'pointer'
    },
    noBoxes: {
        color: '#777',
        fontStyle: 'italic',
        textAlign: 'center',
        marginTop: '10px'
    },
    mapPanel: {
        flex: 1,
        position: 'relative'
    },
    map: {
        width: '100%',
        height: '100%'
    },
    footer: {
        borderTop: '1px solid #ccc',
        padding: '10px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
    },
    selectedInfo: {
        fontWeight: 'bold'
    },
    reserveBtn: {
        padding: '8px 16px',
        borderRadius: '4px',
        border: '1px solid #ccc',
        backgroundColor: '#4caf50',
        color: '#fff',
        cursor: 'pointer'
    }
};

export default EasyboxReservationWidget;
