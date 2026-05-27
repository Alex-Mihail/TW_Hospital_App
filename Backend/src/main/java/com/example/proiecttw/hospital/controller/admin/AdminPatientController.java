package com.example.proiecttw.hospital.controller.admin;
import com.example.proiecttw.hospital.dto.PatientRegisterRequest;
import com.example.proiecttw.hospital.entity.Patient;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.repository.PatientRepository;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/patients")
@CrossOrigin
public class AdminPatientController {

    private final PatientRepository patientRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminPatientController(PatientRepository patientRepo,
                                  AppointmentRepository appointmentRepo, PasswordEncoder passwordEncoder) {
        this.patientRepo = patientRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Patient> getAll() {
        return patientRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getById(@PathVariable Long id) {
        return patientRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/appointments")
    public List<Appointment> getAppointmentsForPatient(@PathVariable Long id) {
        return appointmentRepo.findAllByPatient_Id(id);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody PatientRegisterRequest req) {

        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()
                || req.getLastName() == null || req.getLastName().trim().isEmpty()
                || req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lipsesc câmpuri obligatorii.");
        }

        String username = req.getUsername().trim();

        if (patientRepo.existsByUsername(username)) {
            return ResponseEntity.status(409).body("Username deja folosit.");
        }

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
        p.setPassword(passwordEncoder.encode(req.getPassword())); // encode password
        p.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        p.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        p.setDateOfBirth(req.getDateOfBirth() != null ? req.getDateOfBirth().trim() : null);

        Patient saved = patientRepo.save(p);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Patient updated) {
        Patient existing = patientRepo.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        // username (unique)
        if (updated.getUsername() != null && !updated.getUsername().trim().isEmpty()) {
            String newUsername = updated.getUsername().trim();
            if (!newUsername.equals(existing.getUsername()) && patientRepo.existsByUsername(newUsername)) {
                return ResponseEntity.status(409).body("Username deja folosit.");
            }
            existing.setUsername(newUsername);
        }

        // email (unique)
        if (updated.getEmail() != null && !updated.getEmail().trim().isEmpty()) {
            String newEmail = updated.getEmail().trim();
            if (existing.getEmail() == null || !newEmail.equals(existing.getEmail())) {
                if (patientRepo.existsByEmail(newEmail)) {
                    return ResponseEntity.status(409).body("Email deja folosit.");
                }
            }
            existing.setEmail(newEmail);
        } else if (updated.getEmail() != null) {
            existing.setEmail(null);
        }

        if (updated.getFirstName() != null) existing.setFirstName(updated.getFirstName().trim());
        if (updated.getLastName() != null) existing.setLastName(updated.getLastName().trim());

        if (updated.getPhone() != null) {
            String ph = updated.getPhone().trim();
            existing.setPhone(ph.isEmpty() ? null : ph);
        }

        if (updated.getDateOfBirth() != null) {
            String dob = updated.getDateOfBirth().trim();
            existing.setDateOfBirth(dob.isEmpty() ? null : dob);
        }

        // password
        if (updated.getPassword() != null && !updated.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        Patient saved = patientRepo.save(existing);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!patientRepo.existsById(id)) return ResponseEntity.notFound().build();

        appointmentRepo.deleteAll(appointmentRepo.findAllByPatient_Id(id));
        patientRepo.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
