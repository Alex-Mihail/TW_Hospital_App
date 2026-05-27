package com.example.proiecttw.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String role;
    private String username;
    private String firstName;
    private String lastName;
}