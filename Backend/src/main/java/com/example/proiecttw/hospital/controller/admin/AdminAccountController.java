package com.example.proiecttw.hospital.controller.admin;

import com.example.proiecttw.hospital.dto.LoginRequest;
import com.example.proiecttw.hospital.dto.LoginResponse;
import com.example.proiecttw.hospital.repository.AdminRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/account")
@CrossOrigin
public class AdminAccountController {

    private final AdminRepository adminRepo;

    public AdminAccountController(AdminRepository adminRepo) {
        this.adminRepo = adminRepo;
    }


}
