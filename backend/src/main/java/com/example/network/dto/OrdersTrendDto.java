package com.example.network.dto;

public class OrdersTrendDto {
    private String date;
    private Long count;

    public OrdersTrendDto(String date, Long count) {
        this.date = date;
        this.count = count;
    }

    // Getters and Setters

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
