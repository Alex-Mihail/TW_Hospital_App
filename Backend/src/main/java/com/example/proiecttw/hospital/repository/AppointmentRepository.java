package com.example.proiecttw.hospital.repository;

import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByPatient_Id(Long patientId);

    List<Appointment> findAllByDoctor_Id(Long doctorId);

    long countByPatient_Id(Long patientId);

    List<Appointment> findByDoctor_IdAndStatusInAndAppointmentDatetimeBetween(
            Long doctorId,
            Collection<AppointmentStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByDoctor_IdAndStatusInAndAppointmentDatetime(
            Long doctorId,
            Collection<AppointmentStatus> statuses,
            LocalDateTime appointmentDatetime
    );

    List<Appointment> findByStatusInAndAppointmentDatetimeBefore(
            Collection<AppointmentStatus> statuses,
            LocalDateTime time
    );
}
