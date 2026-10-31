package com.scheduler.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record CreateAppointmentRequest(
        @NotNull @Positive Long providerId,
        @NotNull @Positive Long customerId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
}
