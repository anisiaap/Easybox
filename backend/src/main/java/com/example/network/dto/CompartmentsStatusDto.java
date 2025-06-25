package com.example.network.dto;

public class CompartmentsStatusDto {
    private Long free;
    private Long busy;

    public CompartmentsStatusDto(Long free, Long busy) {
        this.free = free;
        this.busy = busy;
    }


    public Long getFree() {
        return free;
    }

    public void setFree(Long free) {
        this.free = free;
    }

    public Long getBusy() {
        return busy;
    }

    public void setBusy(Long busy) {
        this.busy = busy;
    }
}
