package com.example.proiecttw.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Username/email este obligatoriu.")
    @Size(max = 120)
    private String identifier;

    @NotBlank(message = "Parola este obligatorie.")
    @Size(max = 200)
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
