// src/main/java/com/example/network/messaging/ReservationRequestMessage.java

package com.example.network.messaging;

public class ReservationRequestMessage {
    private String clientName;
    private String phoneNumber;       // storing phone here
    private Long easyboxId;          // desired box
    private String deliveryTime;     // e.g. ISO string
    private Integer minTemperature;
    private Integer totalDimension;
    private Long reservationId;


    // Constructors, getters, setters...

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getEasyboxId() {
        return easyboxId;
    }

    public void setEasyboxId(Long easyboxId) {
        this.easyboxId = easyboxId;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public Integer getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Integer minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Integer getTotalDimension() {
        return totalDimension;
    }

    public void setTotalDimension(Integer totalDimension) {
        this.totalDimension = totalDimension;
    }

    public Long getReservationId() {
        return reservationId;
    }
}
