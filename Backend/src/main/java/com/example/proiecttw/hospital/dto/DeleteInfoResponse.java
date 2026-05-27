package com.example.proiecttw.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteInfoResponse {
    private Long id;
    private String role;              // "PATIENT" or "DOCTOR"
    private long appointmentsCount;   // number of specializations
    private String message;
}
