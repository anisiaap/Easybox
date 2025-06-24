package com.example.easyboxdevice.model;

public class Compartment {
    private Long id;
    private int size;
    private int temperature;
    private String status;    // "free" or "busy"
    private String condition; // "good", "dirty", "broken", etc.

    public Compartment() {}

    public Compartment(Long id, int size, int temperature, String status, String condition) {
        this.id = id;
        this.size = size;
        this.temperature = temperature;
        this.status = status;
        this.condition = condition;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
