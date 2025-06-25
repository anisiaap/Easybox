// src/main/java/com/example/network/entity/Reservation.java

package com.example.network.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("reservation")
public class Reservation {

    @Id
    private Long id;
    private String qrCodeData;

    private LocalDateTime deliveryTime;
    private String status; // "pending", "completed"
    private LocalDateTime expiresAt;      // nullable

    private LocalDateTime reservationStart;

    private LocalDateTime reservationEnd;

    private Long easyboxId;
    @NotNull
    private Long compartmentId;
    private Long userId; // foreign key to user.id

    @Version
    private Long version;  //  for optimistic locking
    private Long bakeryId;

    public Long getBakeryId() {
        return bakeryId;
    }

    public void setBakeryId(Long bakeryId) {
        this.bakeryId = bakeryId;
    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }


    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }
    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
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

    public Long getEasyboxId() {
        return easyboxId;
    }
    public void setEasyboxId(Long easyboxId) {
        this.easyboxId = easyboxId;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }
    public void setCompartmentId(Long compartmentId) {
        this.compartmentId = compartmentId;
    }

    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }

    public void setExpiresAt(LocalDateTime localDateTime) {
        this.expiresAt = localDateTime;
    }

    public void setUserId(Long id) {
        this.userId = id;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

}
