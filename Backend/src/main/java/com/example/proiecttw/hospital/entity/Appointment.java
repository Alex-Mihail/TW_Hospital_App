package com.example.proiecttw.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // patient that has
    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    @JsonIgnoreProperties({"password", "appointments"})
    private Patient patient;

    // chosen doctor
    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    @JsonIgnoreProperties({"password", "appointments"})
    private Doctor doctor;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime appointmentDatetime;

    // short description
    @Column(length = 400)
    private String description;

    // status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
}
