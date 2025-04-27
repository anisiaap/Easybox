package com.example.network.dto;

public  class QrCodeResult {
    private final Long compartmentId;
    private final String newReservationStatus;

    public QrCodeResult(Long compartmentId, String newReservationStatus) {
        this.compartmentId = compartmentId;
        this.newReservationStatus = newReservationStatus;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }

    public String getNewReservationStatus() {
        return newReservationStatus;
    }
}