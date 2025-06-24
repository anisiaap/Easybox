package com.example.easyboxdevice.dto;

public class QrResponse {
    private String result;
    private Long compartmentId;
    private String newReservationStatus;
    private String reason;
    

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }

    public void setCompartmentId(Long compartmentId) {
        this.compartmentId = compartmentId;
    }

    public String getNewReservationStatus() {
        return newReservationStatus;
    }

    public void setNewReservationStatus(String newReservationStatus) {
        this.newReservationStatus = newReservationStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}