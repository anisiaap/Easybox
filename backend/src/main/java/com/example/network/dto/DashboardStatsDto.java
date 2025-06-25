package com.example.network.dto;

public class DashboardStatsDto {
    private long totalEasyboxes;
    private long activeCompartments;
    private long totalOrders;
    private long expiredOrders;

    public DashboardStatsDto(long totalEasyboxes, long activeCompartments, long totalOrders, long expiredOrders) {
        this.totalEasyboxes = totalEasyboxes;
        this.activeCompartments = activeCompartments;
        this.totalOrders = totalOrders;
        this.expiredOrders = expiredOrders;
    }

    public long getTotalEasyboxes() { return totalEasyboxes; }
    public void setTotalEasyboxes(long totalEasyboxes) { this.totalEasyboxes = totalEasyboxes; }

    public long getActiveCompartments() { return activeCompartments; }
    public void setActiveCompartments(long activeCompartments) { this.activeCompartments = activeCompartments; }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getExpiredOrders() { return expiredOrders; }
    public void setExpiredOrders(long expiredOrders) { this.expiredOrders = expiredOrders; }
}
