package com.example.easyboxdevice.controller;

import com.example.easyboxdevice.service.GpioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LockController {
    private static GpioService gpioService;

    @Autowired
    public LockController(GpioService gpioService) {
        LockController.gpioService = gpioService;
    }

    public static void openLock(Long compartmentId) {
        gpioService.openLock(compartmentId);
    }
}

