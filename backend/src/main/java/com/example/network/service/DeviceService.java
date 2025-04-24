package com.example.network.service;

import com.example.network.dto.CompartmentDto; // optional, if needed
import com.example.network.entity.Compartment;
import com.example.network.exception.ConflictException;
import com.example.network.repository.CompartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Service
public class DeviceService {

    private final WebClient webClient;
    private final CompartmentRepository compartmentRepository;

    public DeviceService(
            WebClient.Builder webClientBuilder,
            CompartmentRepository compartmentRepository
    ) {
        this.webClient = webClientBuilder.build();
        this.compartmentRepository = compartmentRepository;
    }

    /**
     * 1) Look up a free compartment in our local DB, using easyboxId + constraints.
     * 2) Mark it busy locally.
     * 3) Call the device to "reserve" it.
     * 4) If device call fails, revert local DB status to "free".
     */
    public Mono<Long> findAndReserveCompartment(String deviceUrl, Long easyboxId, Integer minTemp, Integer totalDim) {
        if (deviceUrl == null || deviceUrl.isEmpty()) {
            return Mono.error(new ConflictException("Device URL is missing or empty."));
        }
        // Step A: Find a suitable compartment from local DB
        return compartmentRepository.findByEasyboxId(easyboxId)
                .filter(this::isSuitableAndFree)    // filter by status & condition
                .filter(comp -> (minTemp == null || comp.getTemperature() >= minTemp))
                .filter(comp -> (totalDim == null || comp.getSize() >= totalDim))
                // you can .sort(...) here if you want the "best" match
                .next() // take the first match
                .switchIfEmpty(Mono.error(new ConflictException("No free compartments available locally.")))
                // Step B: Mark that compartment as busy, so no one else takes it
                .flatMap(comp -> markCompartmentBusy(comp)
                        .flatMap(busyComp -> {
                            // Step C: Call the device to actually reserve it
                            return reserveCompartmentOnDevice(deviceUrl, busyComp.getId())
                                    .flatMap(deviceOk -> {
                                        if (!deviceOk) {
                                            // Step D: revert to free if the device refused
                                            return revertCompartmentToFree(busyComp)
                                                    .then(Mono.error(new ConflictException("Device refused the reservation.")));
                                        }
                                        // All good => return the compartment ID
                                        return Mono.just(busyComp.getId());
                                    });
                        })
                );
    }

    //--------------------------------------------------------------------------------
    // HELPER METHODS
    //--------------------------------------------------------------------------------

    /**
     * Check if compartment is "free" and in a "good" or "clean" condition.
     */
    private boolean isSuitableAndFree(Compartment c) {
        if (!"free".equalsIgnoreCase(c.getStatus())) {
            return false;
        }
        if (c.getCondition() == null) {
            return false;
        }
        String cond = c.getCondition().toLowerCase();
        return ("good".equals(cond) || "clean".equals(cond));
    }

    /**
     * Mark compartment as "busy" in the local DB.
     */
    private Mono<Compartment> markCompartmentBusy(Compartment comp) {
        comp.setStatus("busy");
        return compartmentRepository.save(comp);
    }

    /**
     * Revert compartment to "free" if device reservation fails.
     */
    private Mono<Compartment> revertCompartmentToFree(Compartment comp) {
        comp.setStatus("free");
        return compartmentRepository.save(comp);
    }

    //--------------------------------------------------------------------------------
    // STILL KEEP THE "reserve" and "free" calls to the device
    //--------------------------------------------------------------------------------

    /**
     * Reserve a compartment on the device (POST).
     */
    public Mono<Boolean> reserveCompartmentOnDevice(String deviceUrl, Long compartmentId) {
        if (deviceUrl == null || deviceUrl.isEmpty() || compartmentId == null) {
            return Mono.just(false);
        }
        String reserveUrl = deviceUrl + "/api/ondevice/compartments/" + compartmentId + "/reserve";
        return webClient.post()
                .uri(reserveUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> true)
                .onErrorResume(ex -> Mono.just(false));
    }

    /**
     * Frees a compartment on the device (POST).
     */
    public Mono<Void> freeCompartmentOnDevice(String deviceUrl, Long compartmentId) {
        if (deviceUrl == null || deviceUrl.isEmpty() || compartmentId == null) {
            return Mono.empty();
        }
        String freeUrl = deviceUrl + "/api/ondevice/compartments/" + compartmentId + "/free";
        return webClient.post()
                .uri(freeUrl)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(ex -> Mono.empty()); // ignore error
    }
}
