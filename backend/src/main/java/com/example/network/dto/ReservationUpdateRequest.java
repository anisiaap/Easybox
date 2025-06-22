package com.example.network.dto;

public class ReservationUpdateRequest {
    private String status;
    private Long easyboxId;
    private Long version; // 🆕 Add this field

    public String getStatus() {
        return status;
    }

    public Long getEasyboxId() {
        return easyboxId;
    }

    public Long getVersion() {
        return version;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEasyboxId(Long easyboxId) {
        this.easyboxId = easyboxId;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
