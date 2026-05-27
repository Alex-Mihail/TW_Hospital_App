package com.example.proiecttw.hospital.service;

import com.example.proiecttw.hospital.dto.LoginRequest;
import com.example.proiecttw.hospital.dto.LoginResponse;
import com.example.proiecttw.hospital.entity.Admin;
import com.example.proiecttw.hospital.entity.Doctor;
import com.example.proiecttw.hospital.entity.Patient;
import com.example.proiecttw.hospital.repository.AdminRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;
    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PatientRepository patientRepo,
            DoctorRepository doctorRepo,
            AdminRepository adminRepo,
            PasswordEncoder passwordEncoder) {
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // auto-detect role + login with username OR email
    public LoginResponse login(LoginRequest req) {
        String identifier = req.getIdentifier() == null ? "" : req.getIdentifier().trim();
        String rawPw = req.getPassword() == null ? "" : req.getPassword().trim();

        if (identifier.isEmpty() || rawPw.isEmpty())
            return null;

        // "PATIENT" (username OR email)
        Patient p = patientRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier).orElse(null);
        if (p != null && passwordEncoder.matches(rawPw, p.getPassword())) {
            return new LoginResponse(
                    p.getId(),
                    "PATIENT",
                    p.getUsername(),
                    p.getFirstName(),
                    p.getLastName(),
                    "Login reușit");
        }

        // "DOCTOR" (username OR email)
        Doctor d = doctorRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier).orElse(null);
        if (d != null && passwordEncoder.matches(rawPw, d.getPassword())) {
            return new LoginResponse(
                    d.getId(),
                    "DOCTOR",
                    d.getUsername(),
                    d.getFirstName(),
                    d.getLastName(),
                    "Login reușit");
        }

        // "ADMIN" (username OR email)
        Admin a = adminRepo.findByUsernameOrEmail(identifier, identifier).orElse(null);
        if (a != null && passwordEncoder.matches(rawPw, a.getPassword())) {
            return new LoginResponse(
                    a.getId(),
                    "ADMIN",
                    a.getUsername(),
                    a.getFirstName(),
                    a.getLastName(),
                    "Login reușit");
        }

        return null;
    }
}
