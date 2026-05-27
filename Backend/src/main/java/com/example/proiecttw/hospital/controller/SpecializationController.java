package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.entity.Specialization;
import com.example.proiecttw.hospital.repository.SpecializationRepository;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specializations")
@CrossOrigin
public class SpecializationController {

    private final SpecializationRepository specializationRepo;

    public SpecializationController(SpecializationRepository specializationRepo) {
        this.specializationRepo = specializationRepo;
    }

    // GET all specializations
    @GetMapping
    public List<Specialization> getAll() {
        return specializationRepo.findAll();
    }

    // GET specialization by ID
    @GetMapping("/{id}")
    public ResponseEntity<Specialization> getById(@PathVariable Long id) {
        return specializationRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET specialization by name
    @GetMapping("/by-name")
    public ResponseEntity<Specialization> getByName(@RequestParam String name) {
        return specializationRepo.findByNameIgnoreCase(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE specialization with unique name validation
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Specialization specialization) {

        if (specializationRepo.existsByNameIgnoreCase(specialization.getName())) {
            return ResponseEntity.badRequest()
                    .body("Exista deja o specializare cu numele: " + specialization.getName());
        }

        return ResponseEntity.ok(specializationRepo.save(specialization));
    }

    // UPDATE specialization with unique name validation
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Specialization updated) {

        // verify if specialization is already existing
        return specializationRepo.findById(id)
                .map(existing -> {

                    var existingByName = specializationRepo.findByNameIgnoreCase(updated.getName());

                    if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
                        return ResponseEntity.badRequest()
                                .body("Exista deja o specializare cu numele: " + updated.getName());
                    }

                    updated.setId(id);
                    return ResponseEntity.ok(specializationRepo.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE specialization
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        if (!specializationRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        specializationRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
