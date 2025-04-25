package com.example.network.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

@Component
public class FlywayRepairRunner {

    private final Flyway flyway;

    public FlywayRepairRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @PostConstruct
    public void repair() {
        System.out.println("🔧 Running Flyway repair()...");
        flyway.repair();
    }
}
