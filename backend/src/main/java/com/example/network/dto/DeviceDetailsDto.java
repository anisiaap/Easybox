package com.example.network.dto;

import java.util.List;

public class DeviceDetailsDto {
    private List<CompartmentDto> compartments;
    private PredefinedValuesDto predefinedValues;

    public DeviceDetailsDto() {}

    public DeviceDetailsDto(List<CompartmentDto> compartments, PredefinedValuesDto predefinedValues) {
        this.compartments = compartments;
        this.predefinedValues = predefinedValues;
    }

    public List<CompartmentDto> getCompartments() {
        return compartments;
    }

    public void setCompartments(List<CompartmentDto> compartments) {
        this.compartments = compartments;
    }

    public PredefinedValuesDto getPredefinedValues() {
        return predefinedValues;
    }

    public void setPredefinedValues(PredefinedValuesDto predefinedValues) {
        this.predefinedValues = predefinedValues;
    }
}
