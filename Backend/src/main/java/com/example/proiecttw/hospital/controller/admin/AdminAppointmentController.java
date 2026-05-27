package com.example.proiecttw.hospital.controller.admin;

import com.example.proiecttw.hospital.dto.AdminCreateAppointmentRequest;
import com.example.proiecttw.hospital.dto.UpdateAppointmentStatusRequest;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.AppointmentStatus;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
@CrossOrigin
public class AdminAppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;

    public AdminAppointmentController(
            AppointmentRepository appointmentRepo,
            PatientRepository patientRepo,
            DoctorRepository doctorRepo
    ) {
        this.appointmentRepo = appointmentRepo;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
    }

    @GetMapping
    public List<Appointment> getAll() {
        return appointmentRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getById(@PathVariable Long id) {
        return appointmentRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AdminCreateAppointmentRequest req) {
        if (req.getPatientId() == null || req.getDoctorId() == null || req.getAppointmentDatetime() == null) {
            return ResponseEntity.badRequest().body("patientId, doctorId și appointmentDatetime sunt obligatorii.");
        }

        var patientOpt = patientRepo.findById(req.getPatientId());
        if (patientOpt.isEmpty()) return ResponseEntity.badRequest().body("Pacient inexistent.");

        var doctorOpt = doctorRepo.findById(req.getDoctorId());
        if (doctorOpt.isEmpty()) return ResponseEntity.badRequest().body("Doctor inexistent.");

        Appointment appt = new Appointment();
        appt.setPatient(patientOpt.get());
        appt.setDoctor(doctorOpt.get());
        appt.setAppointmentDatetime(req.getAppointmentDatetime());
        appt.setDescription(req.getDescription());
        appt.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepo.save(appt);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        var apptOpt = appointmentRepo.findById(id);
        if (apptOpt.isEmpty()) return ResponseEntity.notFound().build();

        String st = req.getStatus();
        if (st == null || st.trim().isEmpty()) return ResponseEntity.badRequest().body("Lipsește status.");

        final AppointmentStatus status;
        try {
            status = AppointmentStatus.valueOf(st.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Status invalid: " + st);
        }

        var appt = apptOpt.get();
        appt.setStatus(status);

        return ResponseEntity.ok(appointmentRepo.save(appt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!appointmentRepo.existsById(id)) return ResponseEntity.notFound().build();
        appointmentRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
