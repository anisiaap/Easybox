//// src/main/java/com/example/network/controller/ReservationQueueController.java
//package com.example.network.controller;
//
//import com.example.network.messaging.ReservationRequestMessage;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//@RestController
//@RequestMapping("/api/reservations-queue")
//public class ReservationQueueController {
//
//    private final RabbitTemplate rabbitTemplate;
//
//    public ReservationQueueController(RabbitTemplate rabbitTemplate) {
//        this.rabbitTemplate = rabbitTemplate;
//    }
//
//    @PostMapping
//    public Mono<Void> queueReservation(@RequestBody ReservationRequestMessage msg) {
//        // Publish to "reservation-requests-exchange" with an empty routing key
//        return Mono.fromRunnable(() -> {
//            rabbitTemplate.convertAndSend("reservation-requests-exchange", "", msg);
//        });
//    }
//}
