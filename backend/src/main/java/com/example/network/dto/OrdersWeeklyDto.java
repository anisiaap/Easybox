package com.example.network.dto;

public class OrdersWeeklyDto {
    private String day;
    private long orders;

    public OrdersWeeklyDto(String day, long orders) {
        this.day = day;
        this.orders = orders;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }
}