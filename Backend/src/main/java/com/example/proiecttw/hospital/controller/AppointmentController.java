package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.dto.CreateAppointmentRequest;
import com.example.proiecttw.hospital.dto.UpdateAppointmentStatusRequest;
import com.example.proiecttw.hospital.entity.*;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin
public class AppointmentController {

    private final AppointmentRepository apptRepo;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;

    public AppointmentController(AppointmentRepository apptRepo,
                                 PatientRepository patientRepo,
                                 DoctorRepository doctorRepo) {
        this.apptRepo = apptRepo;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
    }

    private void markFinishedIfNeeded() {
        LocalDateTime nowMinus1h = LocalDateTime.now().minusHours(1);

        var toFinish = apptRepo.findByStatusInAndAppointmentDatetimeBefore(
                List.of(AppointmentStatus.ACCEPTED),
                nowMinus1h
        );

        if (!toFinish.isEmpty()) {
            toFinish.forEach(a -> a.setStatus(AppointmentStatus.FINISHED));
            apptRepo.saveAll(toFinish);
        }
    }

    // GET pacient's appointments
    @GetMapping("/patient/{patientId}")
    public List<Appointment> byPatient(@PathVariable Long patientId) {
        markFinishedIfNeeded();
        return apptRepo.findAllByPatient_Id(patientId)
                .stream()
                .sorted((a, b) -> b.getAppointmentDatetime().compareTo(a.getAppointmentDatetime()))
                .toList();
    }

    // GET doctor's appointments
    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> byDoctor(@PathVariable Long doctorId) {
        markFinishedIfNeeded();
        return apptRepo.findAllByDoctor_Id(doctorId)
                .stream()
                .sorted((a, b) -> b.getAppointmentDatetime().compareTo(a.getAppointmentDatetime()))
                .toList();
    }

    // CREATE appointment BY PACIENT
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateAppointmentRequest req) {
        if (req.getPatientId() == null || req.getDoctorId() == null || req.getAppointmentDatetime() == null) {
            return ResponseEntity.badRequest().body("Lipsesc câmpuri obligatorii.");
        }

        Patient p = patientRepo.findById(req.getPatientId()).orElse(null);
        Doctor d = doctorRepo.findById(req.getDoctorId()).orElse(null);
        if (p == null || d == null) return ResponseEntity.badRequest().body("Patient/Doctor invalid.");

        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(req.getAppointmentDatetime());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("appointmentDatetime invalid. Folosește ISO: 2026-01-02T10:00");
        }

        // validation hours 08:00 - 16:00 (1h slots)
        int hour = dt.getHour();
        if (hour < 8 || hour > 16) {
            return ResponseEntity.badRequest().body("Ora trebuie să fie între 08:00 și 16:00.");
        }
        if (dt.getMinute() != 0) {
            return ResponseEntity.badRequest().body("Minutele trebuie să fie 00 (sloturi de 1 oră).");
        }

        // prevent duplicates (if PENDING/ACCEPTED appointments exist)
        boolean occupied = apptRepo.existsByDoctor_IdAndStatusInAndAppointmentDatetime(
                d.getId(),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.ACCEPTED),
                dt
        );
        if (occupied) {
            return ResponseEntity.status(409).body("Slot ocupat. Alege altă oră.");
        }

        Appointment a = new Appointment();
        a.setPatient(p);
        a.setDoctor(d);
        a.setAppointmentDatetime(dt);
        a.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        a.setStatus(AppointmentStatus.PENDING);

        return ResponseEntity.ok(apptRepo.save(a));
    }

    // DOCTOR: accept/deny
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        if (req.getStatus() == null) return ResponseEntity.badRequest().body("Status lipsă.");

        AppointmentStatus st;
        try {
            st = AppointmentStatus.valueOf(req.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Status invalid. Folosește PENDING/ACCEPTED/DENIED.");
        }

        if (st == AppointmentStatus.PENDING) {
            return ResponseEntity.badRequest().body("Nu seta status înapoi pe PENDING.");
        }

        return apptRepo.findById(id)
                .map(a -> {
                    a.setStatus(st);
                    return ResponseEntity.ok(apptRepo.save(a));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Availability: occupied (if PENDING or ACCEPTED)
    @GetMapping("/availability")
    public ResponseEntity<?> availability(@RequestParam Long doctorId, @RequestParam String date) {
        LocalDate day;
        try {
            day = LocalDate.parse(date);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("date invalid. Folosește YYYY-MM-DD");
        }

        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();

        var appts = apptRepo.findByDoctor_IdAndStatusInAndAppointmentDatetimeBetween(
                doctorId,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.ACCEPTED),
                from,
                to
        );

        return ResponseEntity.ok(appts);
    }

    // cancel appointment by PATIENT
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return apptRepo.findById(id)
                .map(a -> {
                    if (a.getStatus() == AppointmentStatus.DENIED || a.getStatus() == AppointmentStatus.FINISHED) {
                        return ResponseEntity.badRequest().body("Nu poți anula o programare DENIED/FINISHED.");
                    }
                    if (a.getStatus() == AppointmentStatus.CANCELLED) {
                        return ResponseEntity.ok(a);
                    }
                    a.setStatus(AppointmentStatus.CANCELLED);
                    return ResponseEntity.ok(apptRepo.save(a));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // delete appointment by PATIENT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return apptRepo.findById(id)
                .map(a -> {
                    if (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ACCEPTED) {
                        return ResponseEntity.badRequest().body("Nu poți șterge o programare PENDING/ACCEPTED. Folosește Anulează.");
                    }
                    apptRepo.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
