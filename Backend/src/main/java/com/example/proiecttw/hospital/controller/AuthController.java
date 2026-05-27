package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.dto.LoginRequest;
import com.example.proiecttw.hospital.dto.LoginResponse;
import com.example.proiecttw.hospital.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req);
        if (resp == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(resp);
    }
}
