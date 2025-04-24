package com.example.network.dto;

import java.util.List;

public class PredefinedValuesDto {
    private List<PredefinedValueDto> temperature;
    private List<PredefinedValueDto> size;
    private List<PredefinedValueDto> status;
    private List<PredefinedValueDto> condition;

    public PredefinedValuesDto() {
    }

    public PredefinedValuesDto(List<PredefinedValueDto> temperature, List<PredefinedValueDto> size, List<PredefinedValueDto> status, List<PredefinedValueDto> condition) {
        this.temperature = temperature;
        this.size = size;
        this.status = status;
        this.condition = condition;
    }

    public List<PredefinedValueDto> getTemperature() {
        return temperature;
    }

    public void setTemperature(List<PredefinedValueDto> temperature) {
        this.temperature = temperature;
    }

    public List<PredefinedValueDto> getSize() {
        return size;
    }

    public void setSize(List<PredefinedValueDto> size) {
        this.size = size;
    }

    public List<PredefinedValueDto> getStatus() {
        return status;
    }

    public void setStatus(List<PredefinedValueDto> status) {
        this.status = status;
    }

    public List<PredefinedValueDto> getCondition() {
        return condition;
    }

    public void setCondition(List<PredefinedValueDto> condition) {
        this.condition = condition;
    }
}
