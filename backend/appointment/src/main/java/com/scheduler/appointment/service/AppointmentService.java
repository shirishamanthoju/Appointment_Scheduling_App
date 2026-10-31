package com.scheduler.appointment.service;

import com.scheduler.appointment.dto.AppointmentResponse;
import com.scheduler.appointment.dto.CreateAppointmentRequest;
import com.scheduler.appointment.entity.Appointment;
import com.scheduler.appointment.entity.AppointmentStatus;
import com.scheduler.appointment.entity.Provider;
import com.scheduler.appointment.repository.AppointmentRepository;
import com.scheduler.appointment.repository.ProviderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ProviderRepository providerRepository) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public AppointmentResponse book(CreateAppointmentRequest request) {
        validateTimeRange(request.startsAt(), request.endsAt());

        Provider provider = providerRepository.findByIdForUpdate(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider " + request.providerId() + " was not found"));

        boolean overlaps = appointmentRepository.existsOverlappingAppointment(
                provider.getId(),
                AppointmentStatus.BOOKED,
                request.startsAt(),
                request.endsAt());
        if (overlaps) {
            throw new SlotConflictException("Provider already has an appointment in this time range");
        }

        Appointment appointment = new Appointment(
                provider, request.customerId(), request.startsAt(), request.endsAt());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> appointmentNotFound(appointmentId));
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllForProvider(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider " + providerId + " was not found");
        }

        return appointmentRepository.findByProvider_IdOrderByStartsAtAsc(providerId)
                .stream()
                .map(AppointmentService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getProviderSchedule(Long providerId, Instant from, Instant to) {
        validateTimeRange(from, to, "to must be later than from");
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider " + providerId + " was not found");
        }

        return appointmentRepository.findAppointmentsOverlappingWindow(
                        providerId, AppointmentStatus.BOOKED, from, to)
                .stream()
                .map(AppointmentService::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse cancel(Long appointmentId) {
        Long providerId = appointmentRepository.findProviderIdById(appointmentId)
                .orElseThrow(() -> appointmentNotFound(appointmentId));
        providerRepository.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider " + providerId + " was not found"));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> appointmentNotFound(appointmentId));

        if (appointment.getStatus() == AppointmentStatus.BOOKED) {
            appointment.cancel();
        }
        return toResponse(appointment);
    }

    private static void validateTimeRange(Instant startsAt, Instant endsAt) {
        validateTimeRange(startsAt, endsAt, "endsAt must be later than startsAt");
    }

    private static void validateTimeRange(Instant startsAt, Instant endsAt, String message) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new InvalidBookingException(message);
        }
    }

    private static ResourceNotFoundException appointmentNotFound(Long appointmentId) {
        return new ResourceNotFoundException("Appointment " + appointmentId + " was not found");
    }

    private static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getProvider().getId(),
                appointment.getCustomerId(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getStatus());
    }
    
}
