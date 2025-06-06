package com.example.easyboxdevice.config;

import com.example.easyboxdevice.model.Compartment;

import java.util.List;

public class DeviceConfig {
    private List<Compartment> compartments;

    public List<Compartment> getCompartments() {
        return compartments;
    }

    public void setCompartments(List<Compartment> compartments) {
        this.compartments = compartments;
    }
}
