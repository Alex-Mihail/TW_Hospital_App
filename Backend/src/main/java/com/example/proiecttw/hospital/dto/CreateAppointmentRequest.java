package com.example.proiecttw.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAppointmentRequest {

    @NotNull(message = "patientId este obligatoriu.")
    private Long patientId;

    @NotNull(message = "doctorId este obligatoriu.")
    private Long doctorId;

    @NotBlank(message = "appointmentDatetime este obligatoriu.")
    private String appointmentDatetime;

    @Size(max = 400, message = "Descrierea are maxim 400 de caractere.")
    private String description;
}
