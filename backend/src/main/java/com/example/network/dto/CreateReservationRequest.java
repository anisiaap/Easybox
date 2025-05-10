// src/main/java/com/example/network/dto/CreateReservationRequest.java
package com.example.network.dto;

public class CreateReservationRequest {
    private String address;
    private String deliveryTime;  // ISO-8601 datetime in string form
    private Long easyboxId;
    private Integer minTemperature;
    private Integer totalDimension;
    private Long bakeryId;
    private String phone;


    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }

    public Long getEasyboxId() { return easyboxId; }
    public void setEasyboxId(Long easyboxId) { this.easyboxId = easyboxId; }

    public Integer getMinTemperature() { return minTemperature; }
    public void setMinTemperature(Integer minTemperature) { this.minTemperature = minTemperature; }

    public Integer getTotalDimension() { return totalDimension; }
    public void setTotalDimension(Integer totalDimension) { this.totalDimension = totalDimension; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getBakeryId() { return bakeryId; }

    public void setBakeryId(Long bakeryId) {
        this.bakeryId = bakeryId;
    }
}
