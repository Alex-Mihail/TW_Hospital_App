package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.dto.*;
import com.example.proiecttw.hospital.entity.Patient;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.repository.PatientRepository;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.proiecttw.hospital.dto.ResetPasswordRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin
public class PatientController {

    private final PatientRepository patientRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    public PatientController(PatientRepository patientRepo,
            AppointmentRepository appointmentRepo, PasswordEncoder passwordEncoder) {
        this.patientRepo = patientRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Patient> getAll() {
        return patientRepo.findAll()
                .stream()
                .map(doc -> {
                    doc.setUsername(null);
                    doc.setPassword(null);
                    return doc;
                })
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientAccountResponse> getById(@PathVariable Long id) {
        return patientRepo.findById(id)
                .map(p -> ResponseEntity.ok(
                        new PatientAccountResponse(
                                p.getId(),
                                "PATIENT",
                                p.getUsername(),
                                p.getFirstName(),
                                p.getLastName(),
                                p.getEmail(),
                                p.getPhone(),
                                p.getDateOfBirth())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody PatientRegisterRequest req) {

        // minimal validation
        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()
                || req.getLastName() == null || req.getLastName().trim().isEmpty()
                || req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lipsesc câmpuri obligatorii.");
        }

        String username = req.getUsername().trim();

        // unique username
        if (patientRepo.existsByUsername(username)) {
            return ResponseEntity.status(409).body("Username deja folosit.");
        }

        // unique email
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String email = req.getEmail().trim();
            if (patientRepo.existsByEmail(email)) {
                return ResponseEntity.status(409).body("Email deja folosit.");
            }
        }

        Patient p = new Patient();
        p.setFirstName(req.getFirstName().trim());
        p.setLastName(req.getLastName().trim());
        p.setUsername(username);
        p.setPassword(passwordEncoder.encode(req.getPassword()));
        p.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        p.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        p.setDateOfBirth(req.getDateOfBirth() != null ? req.getDateOfBirth().trim() : null);
        Patient saved = patientRepo.save(p);

        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {

        String identifier = req.getIdentifier() == null ? "" : req.getIdentifier().trim();
        String password = req.getPassword() == null ? "" : req.getPassword().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(400).build();
        }

        return patientRepo
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .filter(p -> passwordEncoder.matches(password, p.getPassword()))
                .map(p -> ResponseEntity.ok(
                        new LoginResponse(
                                p.getId(),
                                "PATIENT",
                                p.getUsername(),
                                p.getFirstName(),
                                p.getLastName(),
                                "Login pacient reușit")))
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

        return patientRepo
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .map(p -> {
                    p.setPassword(passwordEncoder.encode(newPw));
                    patientRepo.save(p);
                    return ResponseEntity.ok("Parola pacientului a fost actualizată.");
                })
                .orElse(ResponseEntity.status(404).body("Cont inexistent."));
    }

    // update account
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updateOwnAccount(
            @PathVariable Long id,
            @Valid @RequestBody Patient updated) {
        return patientRepo.findById(id)
                .map(existing -> {

                    // update only allowed fields
                    if (updated.getFirstName() != null)
                        existing.setFirstName(updated.getFirstName().trim());
                    if (updated.getLastName() != null)
                        existing.setLastName(updated.getLastName().trim());

                    // optionals
                    existing.setEmail(updated.getEmail() != null ? updated.getEmail().trim() : null);
                    existing.setPhone(updated.getPhone() != null ? updated.getPhone().trim() : null);
                    existing.setDateOfBirth(updated.getDateOfBirth() != null ? updated.getDateOfBirth().trim() : null);

                    Patient saved = patientRepo.save(existing);
                    saved.setPassword(null); // hide password
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // appointments
    @GetMapping("/{id}/appointments")
    public List<Appointment> getOwnAppointments(@PathVariable Long id) {
        return appointmentRepo.findAllByPatient_Id(id);
    }

    // delete
    @GetMapping("/{id}/delete-info")
    public ResponseEntity<DeleteInfoResponse> getDeleteInfo(@PathVariable Long id) {
        long count = appointmentRepo.countByPatient_Id(id);

        String msg = (count > 0)
                ? "Acest pacient are " + count + " programări. Dacă ștergi contul, toate programările vor fi eliminate."
                : "Ești sigur că vrei să ștergi acest cont?";

        return ResponseEntity.ok(new DeleteInfoResponse(id, "PATIENT", count, msg));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!patientRepo.existsById(id))
            return ResponseEntity.notFound().build();

        appointmentRepo.deleteAll(appointmentRepo.findAllByPatient_Id(id));
        patientRepo.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
