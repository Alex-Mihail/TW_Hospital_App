package com.example.proiecttw.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String role; // "PATIENT", "DOCTOR", "ADMIN"
    private String username;
    private String firstName;
    private String lastName;
    private String message;
}