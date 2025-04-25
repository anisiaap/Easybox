package com.example.easyboxdevice.dto;

public class RegistrationRequest {
    private String address;
     private String clientId; 
    private String status;  // "active" or "inactive"

    public RegistrationRequest() {}

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
