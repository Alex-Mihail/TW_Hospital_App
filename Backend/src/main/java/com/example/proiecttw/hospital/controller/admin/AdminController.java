package com.example.proiecttw.hospital.controller.admin;

import com.example.proiecttw.hospital.dto.LoginRequest;
import com.example.proiecttw.hospital.dto.LoginResponse;
import com.example.proiecttw.hospital.dto.AdminRegisterRequest;
import com.example.proiecttw.hospital.entity.Admin;
import com.example.proiecttw.hospital.repository.AdminRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminRepository adminRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Admin> getAll() {
        return adminRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Admin> getById(@PathVariable Long id) {
        return adminRepo.findById(id)
                .map(a -> { a.setPassword(null); return ResponseEntity.ok(a); })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Admin create(@Valid @RequestBody Admin admin) {
        return adminRepo.save(admin);
    }

    // update general (admin management)
    @PutMapping("/{id}")
    public ResponseEntity<Admin> update(@PathVariable Long id, @Valid @RequestBody Admin updated) {
        return adminRepo.findById(id)
                .map(existing -> {
                    updated.setId(id);

                    if (updated.getPassword() == null || updated.getPassword().trim().isEmpty()) {
                        updated.setPassword(existing.getPassword());
                    } else {
                        updated.setPassword(passwordEncoder.encode(updated.getPassword()));
                    }

                    Admin saved = adminRepo.save(updated);
                    saved.setPassword(null);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterRequest req) {

        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()
                || req.getLastName() == null || req.getLastName().trim().isEmpty()
                || req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lipsesc câmpuri obligatorii.");
        }

        String username = req.getUsername().trim();

        if (adminRepo.existsByUsername(username)) {
            return ResponseEntity.status(409).body("Username deja folosit.");
        }

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String email = req.getEmail().trim();
            if (adminRepo.existsByEmail(email)) {
                return ResponseEntity.status(409).body("Email deja folosit.");
            }
        }

        Admin a = new Admin();
        a.setFirstName(req.getFirstName().trim());
        a.setLastName(req.getLastName().trim());
        a.setUsername(username);
        a.setPassword(passwordEncoder.encode(req.getPassword()));
        a.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);

        Admin saved = adminRepo.save(a);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return adminRepo.findByUsername(req.getIdentifier())
                .filter(a -> passwordEncoder.matches(req.getPassword(), a.getPassword()))
                .map(a -> ResponseEntity.ok(
                        new LoginResponse(
                                a.getId(),
                                "ADMIN",
                                a.getUsername(),
                                a.getFirstName(),
                                a.getLastName(),
                                "Login admin reușit"
                        )
                ))
                .orElse(ResponseEntity.status(401).build());
    }

    @PutMapping("/{id}/account")
    public ResponseEntity<Admin> updateOwnAccount(@PathVariable Long id, @Valid @RequestBody Admin updated) {
        return adminRepo.findById(id)
                .map(existing -> {
                    if (updated.getFirstName() != null) existing.setFirstName(updated.getFirstName().trim());
                    if (updated.getLastName() != null) existing.setLastName(updated.getLastName().trim());
                    existing.setEmail(updated.getEmail() != null ? updated.getEmail().trim() : null);

                    Admin saved = adminRepo.save(existing);
                    saved.setPassword(null);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!adminRepo.existsById(id)) return ResponseEntity.notFound().build();
        adminRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
