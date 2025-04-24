package com.example.network.dto;


public class ReservationQueryRequest {
    private String address;
    private String start;
    private String end;
    private Integer minTemperature;
    private Integer totalDimension;

    // Getters and setters
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getStart() {
        return start;
    }
    public void setStart(String start) {
        this.start = start;
    }
    public String getEnd() {
        return end;
    }
    public void setEnd(String end) {
        this.end = end;
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
}
