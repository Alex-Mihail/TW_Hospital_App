package com.example.proiecttw.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRegisterRequest {

    @NotBlank(message = "Prenumele este obligatoriu.")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu.")
    @Size(max = 80)
    private String lastName;

    @Email(message = "Email invalid.")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Username-ul este obligatoriu.")
    @Size(min = 3, max = 60)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username-ul poate conține doar litere, cifre, '.', '_', '-'.")
    private String username;

    @NotBlank(message = "Parola este obligatorie.")
    @Size(min = 6, max = 200)
    private String password;
}
