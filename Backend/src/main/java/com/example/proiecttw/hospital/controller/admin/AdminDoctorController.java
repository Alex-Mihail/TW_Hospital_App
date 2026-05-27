package com.example.proiecttw.hospital.controller.admin;
import com.example.proiecttw.hospital.dto.DoctorRegisterRequest;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.Doctor;
import com.example.proiecttw.hospital.entity.Specialization;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.SpecializationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
@CrossOrigin
public class AdminDoctorController {

    private final DoctorRepository doctorRepo;
    private final AppointmentRepository appointmentRepo;
    private final SpecializationRepository specializationRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminDoctorController(DoctorRepository doctorRepo,
                                 AppointmentRepository appointmentRepo,
                                 SpecializationRepository specializationRepo,
                                 PasswordEncoder passwordEncoder) {
        this.doctorRepo = doctorRepo;
        this.appointmentRepo = appointmentRepo;
        this.specializationRepo = specializationRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Doctor> getAll() {
        return doctorRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getById(@PathVariable Long id) {
        return doctorRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/appointments")
    public List<Appointment> getAppointmentsForDoctor(@PathVariable Long id) {
        return appointmentRepo.findAllByDoctor_Id(id);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDoctor(@Valid @RequestBody DoctorRegisterRequest req) {

        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()
                || req.getLastName() == null || req.getLastName().trim().isEmpty()
                || req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().trim().isEmpty()
                || req.getSpecializationId() == null) {
            return ResponseEntity.badRequest().body("Lipsesc câmpuri obligatorii.");
        }

        String username = req.getUsername().trim();

        if (doctorRepo.existsByUsername(username)) {
            return ResponseEntity.status(409).body("Username deja folosit.");
        }

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String email = req.getEmail().trim();
            if (doctorRepo.existsByEmail(email)) {
                return ResponseEntity.status(409).body("Email deja folosit.");
            }
        }

        Specialization spec = specializationRepo.findById(req.getSpecializationId()).orElse(null);
        if (spec == null) {
            return ResponseEntity.badRequest().body("Specializare inexistentă.");
        }

        Doctor d = new Doctor();
        d.setFirstName(req.getFirstName().trim());
        d.setLastName(req.getLastName().trim());
        d.setUsername(username);
        d.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        d.setSpecialization(spec);

        d.setPassword(passwordEncoder.encode(req.getPassword()));

        Doctor saved = doctorRepo.save(d);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Doctor updated) {

        Doctor existing = doctorRepo.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        if (updated.getUsername() != null && !updated.getUsername().trim().isEmpty()) {
            String newUsername = updated.getUsername().trim();
            if (!newUsername.equals(existing.getUsername()) && doctorRepo.existsByUsername(newUsername)) {
                return ResponseEntity.status(409).body("Username deja folosit.");
            }
            existing.setUsername(newUsername);
        }

        if (updated.getEmail() != null && !updated.getEmail().trim().isEmpty()) {
            String newEmail = updated.getEmail().trim();
            if (existing.getEmail() == null || !newEmail.equals(existing.getEmail())) {
                if (doctorRepo.existsByEmail(newEmail)) {
                    return ResponseEntity.status(409).body("Email deja folosit.");
                }
            }
            existing.setEmail(newEmail);
        }

        // firstName / lastName
        if (updated.getFirstName() != null) existing.setFirstName(updated.getFirstName().trim());
        if (updated.getLastName() != null) existing.setLastName(updated.getLastName().trim());

        // specialization
        if (updated.getSpecialization() != null && updated.getSpecialization().getId() != null) {
            Long specId = updated.getSpecialization().getId();
            Specialization spec = specializationRepo.findById(specId).orElse(null);
            if (spec == null) return ResponseEntity.badRequest().body("Specializare inexistentă.");
            existing.setSpecialization(spec);
        }

        // password
        if (updated.getPassword() != null && !updated.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        Doctor saved = doctorRepo.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!doctorRepo.existsById(id)) return ResponseEntity.notFound().build();

        appointmentRepo.deleteAll(appointmentRepo.findAllByDoctor_Id(id));
        doctorRepo.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
