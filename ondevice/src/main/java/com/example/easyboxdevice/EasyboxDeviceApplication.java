package com.example.easyboxdevice;

import com.example.easyboxdevice.controller.DeviceDisplayController;
import com.example.easyboxdevice.service.QrScannerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.CommandLineRunner;
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class EasyboxDeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyboxDeviceApplication.class, args);
    }
}


