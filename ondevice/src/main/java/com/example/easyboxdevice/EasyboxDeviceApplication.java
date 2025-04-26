package com.example.easyboxdevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class EasyboxDeviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyboxDeviceApplication.class, args);
    }
}
