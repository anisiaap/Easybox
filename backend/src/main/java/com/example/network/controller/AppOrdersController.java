package com.example.network.controller;

import com.example.network.entity.Easybox;
import com.example.network.entity.Reservation;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

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
    public Flux<Map<String, Object>> getUserOrders(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        List<String> roles = jwt.getClaim("roles");
        boolean isBakery = roles.contains("ROLE_BAKERY");

        Flux<Reservation> reservations = isBakery
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
                            result.put("actionDeadline", isBakery
                                    ? res.getDeliveryTime().toString()
                                    : res.getReservationEnd().toString());

                            return result;
                        })
                        .switchIfEmpty(Mono.fromSupplier(() -> {
                            Map<String, Object> fallback = new HashMap<>();
                            fallback.put("id", res.getId());
                            fallback.put("status", res.getStatus());
                            fallback.put("deliveryTime", res.getDeliveryTime().toString());
                            fallback.put("easyboxAddress", "Unknown Location");
                            return fallback;
                        })))
                ;
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getOrderDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = jwt.getClaim("userId");
        List<String> roles = jwt.getClaim("roles");
        boolean isBakery = roles.contains("ROLE_BAKERY");

        Mono<Reservation> resMono = isBakery
                ? reservationRepository.findByIdAndBakeryId(id, userId)
                : reservationRepository.findByIdAndUserId(id, userId);

        return resMono.flatMap(res ->
                easyboxRepository.findById(res.getEasyboxId())
                        .defaultIfEmpty(new Easybox(null, 0.0, 0.0, null, "inactive"))
                        .map(easybox -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", res.getId());
                            map.put("status", res.getStatus());
                            map.put("deliveryTime", res.getDeliveryTime().toString());
                            map.put("easyboxAddress", easybox.getAddress());
                            map.put("compartmentId", res.getCompartmentId());
                            map.put("actionDeadline", isBakery
                                    ? res.getDeliveryTime().toString()
                                    : res.getReservationEnd().toString());

                            boolean showQr =
                                    (isBakery && "waiting_bakery_drop_off".equalsIgnoreCase(res.getStatus())) ||
                                            (!isBakery && "waiting_client_pick_up".equalsIgnoreCase(res.getStatus()));

                            if (showQr && res.getQrCodeData() != null) {
                                map.put("qrCodeData", res.getQrCodeData());
                            }

                            return map;
                        }));
    }




}
