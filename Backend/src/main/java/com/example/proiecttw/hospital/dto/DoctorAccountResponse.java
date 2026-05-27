package com.example.proiecttw.hospital.dto;

import com.example.proiecttw.hospital.entity.Specialization;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorAccountResponse {
    private Long id;
    private String role;       // "DOCTOR"
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Specialization specialization;
}