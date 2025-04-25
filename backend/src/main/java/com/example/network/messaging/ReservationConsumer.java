//package com.example.network.messaging;
//
//import com.example.network.dto.CreateReservationRequest;
//import com.example.network.service.ReservationService;
//import com.rabbitmq.client.Channel;
//import org.springframework.amqp.rabbit.annotation.RabbitHandler;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.io.IOException;
//
//@Service
//@RabbitListener(queues = "reservation-requests-queue")
//public class ReservationConsumer {
//
//    private final ReservationService reservationService;
//
//    public ReservationConsumer(ReservationService reservationService) {
//        this.reservationService = reservationService;
//    }
//
//    @RabbitHandler
//    public void onMessage(ReservationRequestMessage msg, Channel channel,
//                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
//        // Convert the incoming message to a CreateReservationRequest
//        CreateReservationRequest req = convertMessage(msg);
//
//        // Process the reservation asynchronously
//        reservationService.confirmReservation(msg.getReservationId())
//                .doOnSuccess(reservation -> {
//                    try {
//                        // Acknowledge the message on successful processing.
//                        channel.basicAck(tag, false);
//                    } catch (IOException e) {
//                        System.err.println("Ack error: " + e.getMessage());
//                    }
//                })
//                .doOnError(error -> {
//                    System.err.println("Reservation creation error: " + error.getMessage());
//                    try {
//                        // Negative acknowledge and do not requeue (so DLQ takes it)
//                        channel.basicNack(tag, false, false);
//                    } catch (IOException e) {
//                        System.err.println("Nack error: " + e.getMessage());
//                    }
//                })
//                .subscribe();
//    }
//
//    private CreateReservationRequest convertMessage(ReservationRequestMessage msg) {
//        CreateReservationRequest req = new CreateReservationRequest();
//        req.setClient(msg.getClientName());
//        req.setPhone(msg.getPhoneNumber());
//        req.setEasyboxId(msg.getEasyboxId());
//        req.setDeliveryTime(msg.getDeliveryTime());
//        req.setMinTemperature(msg.getMinTemperature());
//        req.setTotalDimension(msg.getTotalDimension());
//        return req;
//    }
//}
