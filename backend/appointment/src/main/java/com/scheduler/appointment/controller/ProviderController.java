package com.scheduler.appointment.controller;

import com.scheduler.appointment.dto.AppointmentResponse;
import com.scheduler.appointment.dto.CreateProviderRequest;
import com.scheduler.appointment.dto.ProviderResponse;
import com.scheduler.appointment.service.AppointmentService;
import com.scheduler.appointment.service.ProviderService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;
    private final AppointmentService appointmentService;

    public ProviderController(ProviderService providerService, AppointmentService appointmentService) {
        this.providerService = providerService;
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> create(@Valid @RequestBody CreateProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(request));
    }

    @GetMapping(value = "/{providerId}/appointments", params = {"!from", "!to"})
    public List<AppointmentResponse> allAppointments(@PathVariable Long providerId) {
        return appointmentService.getAllForProvider(providerId);
    }

    @GetMapping(value = "/{providerId}/appointments", params = {"from", "to"})
    public List<AppointmentResponse> schedule(
            @PathVariable Long providerId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return appointmentService.getProviderSchedule(providerId, from, to);
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return providerService.getAllProviders();
    }
}
