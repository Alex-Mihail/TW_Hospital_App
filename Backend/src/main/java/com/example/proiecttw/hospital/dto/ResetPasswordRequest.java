package com.example.proiecttw.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Username/email este obligatoriu.")
    @Size(max = 120)
    private String identifier;

    @NotBlank(message = "Parola nouă este obligatorie.")
    @Size(min = 6, max = 200, message = "Parola are minim 6 caractere.")
    private String newPassword;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
