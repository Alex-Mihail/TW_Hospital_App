package com.example.proiecttw.hospital.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCreateAppointmentRequest {
    private Long patientId;
    private Long doctorId;
    private LocalDateTime appointmentDatetime;
    private String description;
}
