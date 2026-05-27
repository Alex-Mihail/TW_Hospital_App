package com.example.proiecttw.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatientAccountResponse {
    private Long id;
    private String role;       // "PATIENT"
    private String username;
    private String firstName;
    private String lastName;

    private String email;
    private String phone;
    private String dateOfBirth;
}
