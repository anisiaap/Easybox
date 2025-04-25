// src/main/java/com/example/network/controller/EasyboxAdminController.java
package com.example.network.controller;

import com.example.network.entity.Easybox;
import com.example.network.repository.EasyboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/admin/easyboxes")
public class EasyboxAdminController {

    @Autowired
    private EasyboxRepository easyboxRepository;


    public EasyboxAdminController(EasyboxRepository easyboxRepository) {
        this.easyboxRepository = easyboxRepository;
    }

    // GET all Easyboxes as a reactive stream
    @GetMapping()
    public Flux<Easybox> getAllEasyboxes() {
        return easyboxRepository.findAll();
    }

}
