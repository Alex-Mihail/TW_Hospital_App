package com.example.proiecttw.hospital.dto;

import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {
    private String status; // "ACCEPTED" / "DENIED"
}
