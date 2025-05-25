package com.example.network.dto;

public class EasyboxDto {
    private Long id;
    private String address;
    private String status;
    private Double latitude;
    private Double longitude;
    private int capacity; // or compartments, or whatever detail you need
    // THESE are the three new fields:
    private boolean available;
    private Double distance;
    private Long freeCompartmentId;

    public EasyboxDto() {}

    public EasyboxDto(Long id, String address, String status, Double latitude, Double longitude, int capacity) {
        this.id = id;
        this.address = address;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    // ADD THESE:

    public boolean isAvailable() {
        return available;
    }
    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Double getDistance() {
        return distance;
    }
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Long getFreeCompartmentId() {
        return freeCompartmentId;
    }
    public void setFreeCompartmentId(Long freeCompartmentId) {
        this.freeCompartmentId = freeCompartmentId;
    }
}
