package com.scheduler.appointment.repository;

import com.scheduler.appointment.entity.Appointment;
import com.scheduler.appointment.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByProvider_IdOrderByStartsAtAsc(Long providerId);

    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.provider.id = :providerId
              and a.status = :status
              and a.startsAt < :requestedEnd
              and a.endsAt > :requestedStart
            """)
    boolean existsOverlappingAppointment(
            @Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("requestedStart") Instant requestedStart,
            @Param("requestedEnd") Instant requestedEnd);

    @Query("""
            select a
            from Appointment a
            where a.provider.id = :providerId
              and a.status = :status
              and a.startsAt < :rangeEnd
              and a.endsAt > :rangeStart
            order by a.startsAt asc
            """)
    List<Appointment> findAppointmentsOverlappingWindow(
            @Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd);

    @Query("select a.provider.id from Appointment a where a.id = :id")
    Optional<Long> findProviderIdById(@Param("id") Long id);
}
