package com.example.network;
import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class EasyboxApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
        System.out.println("TimeZone set to Europe/Bucharest");
    }

    public static void main(String[] args) {
        SpringApplication.run(EasyboxApplication.class, args);
    }
}