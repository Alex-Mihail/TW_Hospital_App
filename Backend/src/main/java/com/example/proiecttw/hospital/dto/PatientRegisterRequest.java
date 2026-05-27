package com.example.proiecttw.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRegisterRequest {

    @NotBlank(message = "Prenumele este obligatoriu.")
    @Size(max = 80, message = "Prenumele are maxim 80 de caractere.")
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu.")
    @Size(max = 80, message = "Numele are maxim 80 de caractere.")
    private String lastName;

    @Email(message = "Email invalid.")
    @Size(max = 120, message = "Emailul are maxim 120 de caractere.")
    private String email;

    @Pattern(regexp = "^$|^[+0-9 ()-]{6,30}$", message = "Telefon invalid.")
    private String phone;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Data nașterii trebuie în format YYYY-MM-DD.")
    private String dateOfBirth;

    @NotBlank(message = "Username-ul este obligatoriu.")
    @Size(min = 3, max = 60, message = "Username-ul are între 3 și 60 de caractere.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username-ul poate conține doar litere, cifre, '.', '_', '-'.")
    private String username;

    @NotBlank(message = "Parola este obligatorie.")
    @Size(min = 6, max = 200, message = "Parola are minim 6 caractere.")
    private String password;
}
