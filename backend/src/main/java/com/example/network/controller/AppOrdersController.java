package com.example.network.controller;

import com.example.network.entity.Easybox;
import com.example.network.entity.Reservation;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/orders")
public class AppOrdersController {

    private final ReservationRepository reservationRepository;
    private final EasyboxRepository easyboxRepository;
    public AppOrdersController(ReservationRepository reservationRepository, EasyboxRepository easyboxRepository) {
        this.reservationRepository = reservationRepository;
        this.easyboxRepository = easyboxRepository;
    }

    @GetMapping
    public Flux<Map<String, Object>> getUserOrders(@RequestParam Long userId, @RequestParam(required = false) String role) {
        Flux<Reservation> reservations = "bakery".equalsIgnoreCase(role)
                ? reservationRepository.findAllByBakeryId(userId)
                : reservationRepository.findAllByUserId(userId);

        return reservations.flatMap(res ->
                easyboxRepository.findById(res.getEasyboxId())
                        .map(easybox -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", res.getId());
                            result.put("status", res.getStatus());
                            result.put("deliveryTime", res.getDeliveryTime().toString());
                            result.put("easyboxAddress", easybox.getAddress());
                            return result;
                        })
                        .switchIfEmpty(Mono.just(new HashMap<>(Map.of(
                                "id", res.getId(),
                                "status", res.getStatus(),
                                "deliveryTime", res.getDeliveryTime().toString(),
                                "easyboxAddress", "Unknown Location"
                        ))))
        );
    }
    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getOrderDetails(
            @PathVariable Long id,
            @RequestParam String role // client, bakery, etc.
    ) {
        return reservationRepository.findById(id)
                .flatMap(res -> easyboxRepository.findById(res.getEasyboxId())
                        .defaultIfEmpty(new Easybox(null, 0.0, 0.0, null, "inactive"))
                        .map(easybox -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", res.getId());
                            map.put("status", res.getStatus());
                            map.put("deliveryTime", res.getDeliveryTime().toString());
                            map.put("easyboxAddress", easybox.getAddress());
                            map.put("compartmentId", res.getCompartmentId());

                            // Only include QR code if allowed
                            boolean showQr =
                                    ("bakery".equalsIgnoreCase(role) && "waiting_bakery_drop_off".equalsIgnoreCase(res.getStatus())) ||
                                            ("client".equalsIgnoreCase(role) && "waiting_client_pick_up".equalsIgnoreCase(res.getStatus()));

                            if (showQr && res.getQrCodeData() != null) {
                                map.put("qrCodeData", res.getQrCodeData());
                            }

                            return map;
                        }));
    }


}
