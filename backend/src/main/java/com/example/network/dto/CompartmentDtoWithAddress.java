
package com.example.network.dto;

public class CompartmentDtoWithAddress {
    private Long id;
    private String status;
    private String condition;
    private int size;
    private int temperature;
    private String easyboxAddress;

    public CompartmentDtoWithAddress(Long id, String status, String condition, int size, int temperature, String easyboxAddress) {
        this.id = id;
        this.status = status;
        this.condition = condition;
        this.size = size;
        this.temperature = temperature;
        this.easyboxAddress = easyboxAddress;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }

    public String getEasyboxAddress() { return easyboxAddress; }
    public void setEasyboxAddress(String easyboxAddress) { this.easyboxAddress = easyboxAddress; }
}
