package com.example.network.dto;

public class ReservationUpdateRequest {
    private String status;
    private Long easyboxId;

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Long getEasyboxId() {
        return easyboxId;
    }
    public void setEasyboxId(Long easyboxId) {
        this.easyboxId = easyboxId;
    }
}
