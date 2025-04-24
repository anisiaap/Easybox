package com.example.network.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("easybox")
public class Easybox {

    @Id
    private Long id;

    private String address;
    private double latitude;
    private double longitude;
    private String status; // e.g., "active"

    // New field for the device’s API URL
    private String deviceUrl;
    @Version
    private Long version;
    public Easybox() {}

    public Easybox(String address, double latitude, double longitude, String status, String deviceUrl) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.deviceUrl = deviceUrl;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getDeviceUrl() {
        return deviceUrl;
    }
    public void setDeviceUrl(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }
}
