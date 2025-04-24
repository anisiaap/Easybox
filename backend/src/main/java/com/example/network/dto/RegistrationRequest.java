package com.example.network.dto;

public class RegistrationRequest {
    private String address;
    private String deviceUrl;
    private String status;  // "active" or "inactive"

    public RegistrationRequest() {}

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getDeviceUrl() {
        return deviceUrl;
    }
    public void setDeviceUrl(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
