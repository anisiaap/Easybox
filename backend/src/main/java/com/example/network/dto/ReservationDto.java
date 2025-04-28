package com.example.network.dto;

public class ReservationDto {
    private Long id;
    private String userPhone;
    private String bakeryName;
    private String easyboxAddress;
    private String status;

    public ReservationDto(Long id, String userPhone, String bakeryName, String easyboxAddress, String status) {
        this.id = id;
        this.userPhone = userPhone;
        this.bakeryName = bakeryName;
        this.easyboxAddress = easyboxAddress;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getBakeryName() { return bakeryName; }
    public void setBakeryName(String bakeryName) { this.bakeryName = bakeryName; }

    public String getEasyboxAddress() { return easyboxAddress; }
    public void setEasyboxAddress(String easyboxAddress) { this.easyboxAddress = easyboxAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
