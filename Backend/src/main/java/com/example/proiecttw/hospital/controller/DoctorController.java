package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.dto.DoctorAccountResponse;
import com.example.proiecttw.hospital.dto.LoginRequest;
import com.example.proiecttw.hospital.dto.LoginResponse;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.Doctor;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.proiecttw.hospital.dto.ResetPasswordRequest;
import jakarta.validation.Valid;

import javax.swing.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin
public class DoctorController {

    private final DoctorRepository doctorRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    public DoctorController(DoctorRepository doctorRepo,
                            AppointmentRepository appointmentRepo,  PasswordEncoder passwordEncoder) {
        this.doctorRepo = doctorRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Doctor> getAll() {
        return doctorRepo.findAll()
                .stream()
                .map(doc -> {
                    doc.setUsername(null);
                    doc.setPassword(null);
                    return doc;
                })
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorAccountResponse> getById(@PathVariable Long id) {
        return doctorRepo.findById(id)
                .map(d -> ResponseEntity.ok(
                        new DoctorAccountResponse(
                                d.getId(),
                                "DOCTOR",
                                d.getUsername(),
                                d.getFirstName(),
                                d.getLastName(),
                                d.getEmail(),
                                d.getSpecialization()
                        )
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-specialization")
    public List<Doctor> getBySpec(@RequestParam String specialization) {
        return doctorRepo.findBySpecialization_NameIgnoreCase(specialization);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {

        String identifier = req.getIdentifier() == null ? "" : req.getIdentifier().trim();
        String password = req.getPassword() == null ? "" : req.getPassword().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(400).build();
        }

        return doctorRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .filter(d -> passwordEncoder.matches(password, d.getPassword()))
                .map(d -> ResponseEntity.ok(
                        new LoginResponse(
                                d.getId(),
                                "DOCTOR",
                                d.getUsername(),
                                d.getFirstName(),
                                d.getLastName(),
                                "Login doctor reușit"
                        )
                ))
                .orElse(ResponseEntity.status(401).build());
    }

    @PutMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {

        if (req == null || req.getIdentifier() == null || req.getIdentifier().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Completează username/email.");
        }

        if (req.getNewPassword() == null || req.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Completează parola nouă.");
        }

        String identifier = req.getIdentifier().trim();
        String newPw = req.getNewPassword().trim();

        if (newPw.length() < 6) {
            return ResponseEntity.badRequest().body("Parola trebuie să aibă minim 6 caractere.");
        }

        return doctorRepo
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .map(d -> {
                    d.setPassword(passwordEncoder.encode(newPw));
                    doctorRepo.save(d);
                    return ResponseEntity.ok("Parola doctorului a fost actualizată.");
                })
                .orElse(ResponseEntity.status(404).body("Cont inexistent."));
    }

    @GetMapping("/{id}/account")
    public ResponseEntity<Doctor> getOwnAccount(@PathVariable Long id) {
        return doctorRepo.findById(id)
                .map(doc -> {
                    doc.setPassword(null);
                    return ResponseEntity.ok(doc);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // edit account
    @PutMapping("/{id}/account")
    public ResponseEntity<Doctor> updateOwnAccount(
            @PathVariable Long id,
            @Valid @RequestBody Doctor updated
    ) {
        return doctorRepo.findById(id)
                .map(existing -> {

                    // first name / last name
                    if (updated.getFirstName() != null)
                        existing.setFirstName(updated.getFirstName().trim());

                    if (updated.getLastName() != null)
                        existing.setLastName(updated.getLastName().trim());

                    // email (can be null)
                    if (updated.getEmail() != null) {
                        String email = updated.getEmail().trim();
                        existing.setEmail(email.isEmpty() ? null : email);
                    }

                    Doctor saved = doctorRepo.save(existing);

                    // not exposing password (hash)
                    saved.setPassword(null);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/appointments")
    public List<Appointment> getOwnAppointments(@PathVariable Long id) {
        return appointmentRepo.findAllByDoctor_Id(id);
    }
}
