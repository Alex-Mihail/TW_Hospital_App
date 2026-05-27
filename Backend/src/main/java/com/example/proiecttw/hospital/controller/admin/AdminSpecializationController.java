package com.example.proiecttw.hospital.controller.admin;

import com.example.proiecttw.hospital.entity.Specialization;
import com.example.proiecttw.hospital.repository.SpecializationRepository;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.proiecttw.hospital.repository.DoctorRepository;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/specializations")
@CrossOrigin
public class AdminSpecializationController {

    private final SpecializationRepository specializationRepo;
    private final DoctorRepository doctorRepo;

    public AdminSpecializationController(SpecializationRepository specializationRepo,  DoctorRepository doctorRepo) {
        this.specializationRepo = specializationRepo;
        this.doctorRepo = doctorRepo;
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

    // CREATE specialization with unique validation
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Specialization specialization) {

        if (specializationRepo.existsByNameIgnoreCase(specialization.getName())) {
            return ResponseEntity.badRequest()
                    .body("Exista deja o specializare cu numele: " + specialization.getName());
        }

        return ResponseEntity.ok(specializationRepo.save(specialization));
    }

    // UPDATE specialization with unique validation
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody Specialization updated) {

        // Check if specialization exists
        return specializationRepo.findById(id)
                .map(existing -> {

                    // Check if another specialization has the same name
                    var specByName = specializationRepo.findByNameIgnoreCase(updated.getName());

                    if (specByName.isPresent() && !specByName.get().getId().equals(id)) {
                        return ResponseEntity.badRequest()
                                .body("Exista deja o specializare cu numele: " + updated.getName());
                    }

                    updated.setId(id);
                    return ResponseEntity.ok(specializationRepo.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var spec = specializationRepo.findById(id).orElse(null);
        if (spec == null) return ResponseEntity.notFound().build();

        var docs = doctorRepo.findBySpecialization_Id(id);
        for (var d : docs) d.setSpecialization(null);
        doctorRepo.saveAll(docs);

        specializationRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
