package com.example.network;
import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EasyboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyboxApplication.class, args);
    }
}
