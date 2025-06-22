package com.example.network.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("bakery")
public class Bakery {

    @Id
    private Long id;
    @Version
    private Long version;
    private String name;
    private String phone;
    @Column("pluginInstalled")
    private Boolean pluginInstalled;
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    private String token;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }


    public Boolean getPluginInstalled() {
        return pluginInstalled;
    }
    public void setPluginInstalled(Boolean pluginInstalled) {
        this.pluginInstalled = pluginInstalled;
    }
}
