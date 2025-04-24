import React from "react";
import { createRoot } from "react-dom/client"; // Correct import for React 18
import EasyboxReservationWidget from "./EasyboxReservationWidget";
import UiErrorBoundary from './UiErrorBoundary';

// Expose a global function for external use
window.initEasyboxWidget = ({ containerId, apiUrl, clientAddress, additionalCriteria, onReservationSelect }) => {
    const container = document.getElementById(containerId);
    if (!container) {
        console.error("Container not found:", containerId);
        return;
    }

    // Ensure createRoot() is only called once per container
    if (!container._reactRootContainer) {
        container._reactRootContainer = createRoot(container); // Store root reference
    }

    // Use the existing root instance for updates
    container._reactRootContainer.render(
        <UiErrorBoundary>
            <EasyboxReservationWidget
                apiUrl={apiUrl}
                clientAddress={clientAddress}
                additionalCriteria={additionalCriteria}
                onReservationSelect={onReservationSelect}
            />
        </UiErrorBoundary>
    );
};
