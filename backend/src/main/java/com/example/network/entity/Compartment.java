package com.example.network.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("compartment")
public class Compartment {

    @Id
    private Long id;
    private Long easyboxId;
    private int size;
    private int temperature;
    private String status;    // e.g., "free", "busy"
    private String condition; // e.g., "good", "dirty", "broken"

    @Version
    private Long version;  // optional, if you added a version column

    // Constructors
    public Compartment() {}

    public Compartment(Long id, Long easyboxId, int size, int temperature,
                       String status, String condition) {
        this.id = id;
        this.easyboxId = easyboxId;
        this.size = size;
        this.temperature = temperature;
        this.status = status;
        this.condition = condition;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEasyboxId() { return easyboxId; }
    public void setEasyboxId(Long easyboxId) { this.easyboxId = easyboxId; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
