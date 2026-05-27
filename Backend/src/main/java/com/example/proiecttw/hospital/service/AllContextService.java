package com.example.proiecttw.hospital.service;

import com.example.proiecttw.hospital.dto.ChatRequest;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.Doctor;
import com.example.proiecttw.hospital.entity.Specialization;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.SpecializationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AllContextService {

    private final AppointmentRepository apptRepo;
    private final DoctorRepository doctorRepo;
    private final SpecializationRepository specRepo;

    public AllContextService(AppointmentRepository apptRepo,
                             DoctorRepository doctorRepo,
                             SpecializationRepository specRepo) {
        this.apptRepo = apptRepo;
        this.doctorRepo = doctorRepo;
        this.specRepo = specRepo;
    }

    public String buildContext(String role, Long userId, ChatRequest.UiContext ui) {
        StringBuilder ctx = new StringBuilder();

        // UI context from frontend
        ctx.append(buildUiSection(ui)).append("\n\n");

        // specializations
        ctx.append(buildSpecializationsSection()).append("\n");

        // doctors (limited by logged in account)
        ctx.append(buildDoctorsSection(80)).append("\n");

        // user's appointments (limited by logged in account)
        if (userId != null && role != null && !role.isBlank()) {
            ctx.append(buildUserAppointmentsSection(role, userId, 10)).append("\n");
        } else {
            ctx.append("PROGRAMĂRILE UTILIZATORULUI: (utilizator neautentificat)\n\n");
        }

        ctx.append("""
        REGULI STRICTE:
        - Răspunde DOAR pe baza informațiilor din CONTEXT.
        - Nu inventa doctori, specializări, ore, date sau programări.
        - Nu afișa ID-uri tehnice (id programare / id pacient / id doctor).
        - Nu oferi diagnostic/tratament.
        - Dacă întrebarea e despre "cum folosesc aplicația", folosește UI_CONTEXT.
        """);

        return ctx.toString();
    }

    private String buildUiSection(ChatRequest.UiContext ui) {
        if (ui == null) {
            return "UI_CONTEXT: (nu a fost furnizat din frontend)";
        }
        StringBuilder sb = new StringBuilder("UI_CONTEXT:\n");
        sb.append("- Page: ").append(ui.page() != null ? ui.page() : "necunoscut").append("\n");

        if (ui.actions() != null && !ui.actions().isEmpty()) {
            sb.append("- Acțiuni disponibile:\n");
            for (String a : ui.actions()) sb.append("  • ").append(a).append("\n");
        }

        if (ui.steps() != null && !ui.steps().isEmpty()) {
            sb.append("- Pași sugerați în UI:\n");
            for (int i = 0; i < ui.steps().size(); i++) {
                sb.append("  ").append(i + 1).append(") ").append(ui.steps().get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildSpecializationsSection() {
        List<Specialization> specs = specRepo.findAll();
        if (specs.isEmpty()) return "SPECIALIZĂRI: (nu există în sistem)\n";

        StringBuilder sb = new StringBuilder("SPECIALIZĂRI disponibile:\n");
        for (Specialization s : specs) {
            if (s != null && s.getName() != null) sb.append("- ").append(s.getName()).append("\n");
        }
        return sb.toString();
    }

    private String buildDoctorsSection(int limit) {
        List<Doctor> docs = doctorRepo.findAll();
        if (docs.isEmpty()) return "DOCTORI: (nu există în sistem)\n";

        docs = docs.stream().limit(limit).toList();

        StringBuilder sb = new StringBuilder("DOCTORI disponibili (max " + limit + "):\n");
        for (Doctor d : docs) {
            if (d == null) continue;

            String specName = (d.getSpecialization() != null && d.getSpecialization().getName() != null)
                    ? d.getSpecialization().getName()
                    : "fără specializare";

            sb.append("- Dr. ")
                    .append(safe(d.getFirstName())).append(" ").append(safe(d.getLastName()))
                    .append(" | Specializare: ").append(specName);

            if (d.getEmail() != null && !d.getEmail().isBlank()) {
                sb.append(" | Email: ").append(d.getEmail().trim());
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildUserAppointmentsSection(String role, Long userId, int limit) {
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
                    return db.compareTo(da);
                })
                .limit(limit)
                .toList();

        if (appts.isEmpty()) return "PROGRAMĂRILE UTILIZATORULUI: nu există programări.\n";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy 'la' HH:mm");
        StringBuilder sb = new StringBuilder("PROGRAMĂRILE utilizatorului (max " + limit + "):\n");

        for (Appointment a : appts) {
            String doctorPretty = "Doctor necunoscut";
            if (a.getDoctor() != null && a.getDoctor().getId() != null) {
                doctorPretty = doctorRepo.findById(a.getDoctor().getId())
                        .map(d -> {
                            String name = "Dr. " + safe(d.getFirstName()) + " " + safe(d.getLastName());
                            if (d.getSpecialization() != null && d.getSpecialization().getName() != null) {
                                name += " (" + d.getSpecialization().getName() + ")";
                            }
                            return name.trim();
                        })
                        .orElse("Doctor necunoscut");
            }

            sb.append("- ")
                    .append(a.getAppointmentDatetime() != null ? a.getAppointmentDatetime().format(fmt) : "dată necunoscută")
                    .append(", cu ").append(doctorPretty)
                    .append(", status ").append(prettyStatus(String.valueOf(a.getStatus())));

            if (a.getDescription() != null && !a.getDescription().isBlank()) {
                sb.append(", pentru ").append(a.getDescription().trim());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

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
