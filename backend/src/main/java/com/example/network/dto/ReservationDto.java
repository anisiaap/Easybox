package com.example.network.dto;

import java.time.LocalDateTime;

public class ReservationDto {
    private Long id;
    private String userPhone;
    private String bakeryName;
    private String easyboxAddress;
    private String status;

    private Long compartmentId;
    private LocalDateTime reservationStart;
    private LocalDateTime reservationEnd;

    public ReservationDto(Long id, String userPhone, String bakeryName, String easyboxAddress,
                          String status, Long compartmentId, LocalDateTime reservationStart, LocalDateTime reservationEnd) {
        this.id = id;
        this.userPhone = userPhone;
        this.bakeryName = bakeryName;
        this.easyboxAddress = easyboxAddress;
        this.status = status;
        this.compartmentId = compartmentId;
        this.reservationStart = reservationStart;
        this.reservationEnd = reservationEnd;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }

    public void setCompartmentId(Long compartmentId) {
        this.compartmentId = compartmentId;
    }

    public LocalDateTime getReservationStart() {
        return reservationStart;
    }

    public void setReservationStart(LocalDateTime reservationStart) {
        this.reservationStart = reservationStart;
    }

    public LocalDateTime getReservationEnd() {
        return reservationEnd;
    }

    public void setReservationEnd(LocalDateTime reservationEnd) {
        this.reservationEnd = reservationEnd;
    }

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
