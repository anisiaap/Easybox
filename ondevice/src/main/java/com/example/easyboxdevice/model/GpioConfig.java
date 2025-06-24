package com.example.easyboxdevice.model;

import java.util.List;

public class GpioConfig {
    private List<GpioMapping> mappings;

    public List<GpioMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<GpioMapping> mappings) {
        this.mappings = mappings;
    }
}
