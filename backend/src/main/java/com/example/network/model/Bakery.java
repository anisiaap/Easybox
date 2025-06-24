package com.example.network.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("bakery")
public class Bakery {

    @Id
    private Long id;

    private String name;
    private String phone;
    private Boolean pluginInstalled;
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    private String token;

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

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
