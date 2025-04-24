package com.example.network.dto;

public class PredefinedValueDto {
    private String value;
    private String description;

    public PredefinedValueDto() {
    }

    public PredefinedValueDto(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
