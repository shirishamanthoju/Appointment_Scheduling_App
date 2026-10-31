package com.scheduler.appointment.dto;

import com.scheduler.appointment.entity.AppointmentStatus;
import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long providerId,
        Long customerId,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status) {
}
