package com.example.proiecttw.hospital.service;

import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentContextService {

    private final AppointmentRepository apptRepo;
    private final DoctorRepository doctorRepo;

    public AppointmentContextService(AppointmentRepository apptRepo, DoctorRepository doctorRepo) {
        this.apptRepo = apptRepo;
        this.doctorRepo = doctorRepo;
    }

    public String buildContext(String role, Long userId) {
        if (role == null || userId == null) {
            return "Context invalid (role/userId lipsă).";
        }

        String r = role.trim().toUpperCase();

        List<Appointment> appts = r.equals("DOCTOR")
                ? apptRepo.findAllByDoctor_Id(userId)
                : apptRepo.findAllByPatient_Id(userId);

        appts = appts.stream()
                .sorted((a, b) -> {
                    LocalDateTime da = a.getAppointmentDatetime();
                    LocalDateTime db = b.getAppointmentDatetime();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da); // desc
                })
                .limit(10)
                .toList();

        if (appts.isEmpty()) {
            return "Nu există programări pentru acest utilizator.";
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy 'la' HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("PROGRAMĂRI (max 10). Nu include ID-uri.\n");

        for (Appointment a : appts) {
            LocalDateTime dt = a.getAppointmentDatetime();

            // doctor's name + specialization
            String doctorPretty = doctorRepo.findById(a.getDoctor().getId())
                    .map(d -> {
                        String name = "Dr. " + safe(d.getFirstName()) + " " + safe(d.getLastName());
                        if (d.getSpecialization() != null && d.getSpecialization().getName() != null) {
                            name += " (" + d.getSpecialization().getName() + ")";
                        }
                        return name.trim();
                    })
                    .orElse("Doctor necunoscut");

            sb.append("- ")
                    .append(dt != null ? dt.format(fmt) : "dată necunoscută")
                    .append(", cu ").append(doctorPretty)
                    .append(", status ").append(prettyStatus(String.valueOf(a.getStatus())));

            if (a.getDescription() != null && !a.getDescription().isBlank()) {
                sb.append(", pentru ").append(a.getDescription().trim());
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String prettyStatus(String status) {
        if (status == null) return "necunoscut";
        return switch (status.trim().toUpperCase()) {
            case "PENDING" -> "în așteptare";
            case "ACCEPTED" -> "acceptată";
            case "DENIED" -> "respinsă";
            case "CANCELLED" -> "anulată";
            case "FINISHED" -> "finalizată";
            default -> status.toLowerCase();
        };
    }
}
