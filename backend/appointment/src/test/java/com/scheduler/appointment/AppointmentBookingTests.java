package com.scheduler.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scheduler.appointment.dto.AppointmentResponse;
import com.scheduler.appointment.dto.CreateAppointmentRequest;
import com.scheduler.appointment.entity.AppointmentStatus;
import com.scheduler.appointment.entity.Provider;
import com.scheduler.appointment.repository.AppointmentRepository;
import com.scheduler.appointment.repository.ProviderRepository;
import com.scheduler.appointment.service.AppointmentService;
import com.scheduler.appointment.service.InvalidBookingException;
import com.scheduler.appointment.service.SlotConflictException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppointmentBookingTests {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProviderRepository providerRepository;

    private Provider provider;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        providerRepository.deleteAll();
        provider = providerRepository.save(new Provider("Test Provider"));
    }

    @Test
    void allowsAdjacentAppointmentsAndRejectsOverlaps() {
        appointmentService.book(request("2030-01-01T10:00:00Z", "2030-01-01T11:00:00Z"));
        AppointmentResponse adjacent = appointmentService.book(
                request("2030-01-01T11:00:00Z", "2030-01-01T12:00:00Z"));

        assertEquals(AppointmentStatus.BOOKED, adjacent.status());
        assertThrows(SlotConflictException.class,
                () -> appointmentService.book(request("2030-01-01T10:30:00Z", "2030-01-01T11:30:00Z")));
    }

    @Test
    void cancelledAppointmentNoLongerBlocksItsTime() {
        AppointmentResponse booked = appointmentService.book(
                request("2030-02-01T10:00:00Z", "2030-02-01T11:00:00Z"));

        AppointmentResponse cancelled = appointmentService.cancel(booked.id());
        AppointmentResponse replacement = appointmentService.book(
                request("2030-02-01T10:00:00Z", "2030-02-01T11:00:00Z"));

        assertEquals(AppointmentStatus.CANCELLED, cancelled.status());
        assertEquals(AppointmentStatus.BOOKED, replacement.status());
    }

    @Test
    void rejectsNonPositiveTimeRange() {
        assertThrows(InvalidBookingException.class,
                () -> appointmentService.book(request("2030-03-01T11:00:00Z", "2030-03-01T11:00:00Z")));
        assertThrows(InvalidBookingException.class,
                () -> appointmentService.book(request("2030-03-01T12:00:00Z", "2030-03-01T11:00:00Z")));
    }

    @Test
    void providerScheduleReturnsActiveAppointmentsOverlappingRequestedWindowInStartOrder() {
        AppointmentResponse beforeWindow = appointmentService.book(
                request("2030-03-31T09:00:00Z", "2030-03-31T09:30:00Z"));
        AppointmentResponse overlapsWindowStart = appointmentService.book(
                request("2030-03-31T09:30:00Z", "2030-03-31T10:30:00Z"));
        AppointmentResponse insideWindow = appointmentService.book(
                request("2030-03-31T10:30:00Z", "2030-03-31T11:00:00Z"));
        appointmentService.book(request("2030-03-31T12:00:00Z", "2030-03-31T13:00:00Z"));
        appointmentService.cancel(insideWindow.id());

        List<AppointmentResponse> schedule = appointmentService.getProviderSchedule(
                provider.getId(),
                Instant.parse("2030-03-31T10:00:00Z"),
                Instant.parse("2030-03-31T12:00:00Z"));

        assertEquals(List.of(overlapsWindowStart.id()), schedule.stream()
                .map(AppointmentResponse::id)
                .toList());
        assertFalse(schedule.stream().anyMatch(appointment -> appointment.id().equals(beforeWindow.id())));
    }

    @Test
    void providerAppointmentsWithoutRangeReturnsAllStatusesInStartOrder() {
        AppointmentResponse later = appointmentService.book(
                request("2030-03-30T11:00:00Z", "2030-03-30T12:00:00Z"));
        AppointmentResponse earlier = appointmentService.book(
                request("2030-03-30T09:00:00Z", "2030-03-30T10:00:00Z"));
        appointmentService.cancel(earlier.id());

        List<AppointmentResponse> appointments = appointmentService.getAllForProvider(provider.getId());

        assertEquals(List.of(earlier.id(), later.id()), appointments.stream()
                .map(AppointmentResponse::id)
                .toList());
        assertEquals(AppointmentStatus.CANCELLED, appointments.get(0).status());
    }

    @Test
    void simultaneousBookingsForSameProviderCannotBothSucceed() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() -> tryBookAfterStart(ready, start));
            Future<Boolean> second = executor.submit(() -> tryBookAfterStart(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean firstBooked = first.get(10, TimeUnit.SECONDS);
            boolean secondBooked = second.get(10, TimeUnit.SECONDS);
            assertFalse(firstBooked && secondBooked);
            assertEquals(1, appointmentRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean tryBookAfterStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            appointmentService.book(request("2030-04-01T10:00:00Z", "2030-04-01T11:00:00Z"));
            return true;
        } catch (SlotConflictException exception) {
            return false;
        }
    }

    private CreateAppointmentRequest request(String startsAt, String endsAt) {
        return new CreateAppointmentRequest(
                provider.getId(),
                100L,
                Instant.parse(startsAt),
                Instant.parse(endsAt));
    }
}
