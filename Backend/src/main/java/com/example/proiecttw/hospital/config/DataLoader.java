package com.example.proiecttw.hospital.config;

import com.example.proiecttw.hospital.entity.Admin;
import com.example.proiecttw.hospital.entity.Appointment;
import com.example.proiecttw.hospital.entity.AppointmentStatus;
import com.example.proiecttw.hospital.entity.Doctor;
import com.example.proiecttw.hospital.entity.Patient;
import com.example.proiecttw.hospital.entity.Specialization;
import com.example.proiecttw.hospital.repository.AdminRepository;
import com.example.proiecttw.hospital.repository.AppointmentRepository;
import com.example.proiecttw.hospital.repository.DoctorRepository;
import com.example.proiecttw.hospital.repository.PatientRepository;
import com.example.proiecttw.hospital.repository.SpecializationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final SpecializationRepository specRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final AdminRepository adminRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(SpecializationRepository specRepo,
                      DoctorRepository doctorRepo,
                      PatientRepository patientRepo,
                      AdminRepository adminRepo,
                      AppointmentRepository appointmentRepo,
                      PasswordEncoder passwordEncoder) {
        this.specRepo = specRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.adminRepo = adminRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedSpecializations();
        seedAdmins();
        seedDoctors();
        seedPatients();
        seedAppointments();
    }

    private void seedSpecializations() {
        if (specRepo.count() > 0) return;

        List<Specialization> specs = List.of(
                spec("Cardiologie", "Evaluare și tratament al afecțiunilor cardiovasculare."),
                spec("Dermatologie", "Diagnostic și tratament pentru afecțiunile pielii."),
                spec("Pediatrie", "Îngrijire medicală pentru copii și adolescenți."),
                spec("Ortopedie", "Tratament pentru sistemul musculo-scheletic."),
                spec("Neurologie", "Diagnostic pentru afecțiuni ale sistemului nervos."),
                spec("Oftalmologie", "Examinări și tratamente pentru ochi."),
                spec("ORL", "Tratament pentru ureche, nas și gât."),
                spec("Medicină internă", "Diagnostic și tratament pentru afecțiuni interne.")
        );
        specRepo.saveAll(specs);
    }

    private Specialization spec(String name, String description) {
        Specialization s = new Specialization();
        s.setName(name);
        s.setDescription(description);
        return s;
    }

    private void seedAdmins() {
        if (adminRepo.count() > 0) return;

        Admin admin = new Admin();
        admin.setFirstName("Admin");
        admin.setLastName("Principal");
        admin.setEmail("admin@tw-hospital.ro");
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        adminRepo.save(admin);
    }

    private void seedDoctors() {
        if (doctorRepo.count() > 0) return;

        Specialization cardiologie = specRepo.findByNameIgnoreCase("Cardiologie").orElse(null);
        Specialization dermatologie = specRepo.findByNameIgnoreCase("Dermatologie").orElse(null);
        Specialization pediatrie = specRepo.findByNameIgnoreCase("Pediatrie").orElse(null);
        Specialization ortopedie = specRepo.findByNameIgnoreCase("Ortopedie").orElse(null);
        Specialization neurologie = specRepo.findByNameIgnoreCase("Neurologie").orElse(null);
        Specialization oftalmologie = specRepo.findByNameIgnoreCase("Oftalmologie").orElse(null);

        doctorRepo.saveAll(List.of(
                doctor("Andrei", "Popescu", "andrei.popescu@tw-hospital.ro", "dr.popescu", "doctor123", cardiologie),
                doctor("Maria", "Ionescu", "maria.ionescu@tw-hospital.ro", "dr.ionescu", "doctor123", dermatologie),
                doctor("Cristian", "Stan", "cristian.stan@tw-hospital.ro", "dr.stan", "doctor123", pediatrie),
                doctor("Elena", "Dumitru", "elena.dumitru@tw-hospital.ro", "dr.dumitru", "doctor123", ortopedie),
                doctor("Vlad", "Marinescu", "vlad.marinescu@tw-hospital.ro", "dr.marinescu", "doctor123", neurologie),
                doctor("Ioana", "Georgescu", "ioana.georgescu@tw-hospital.ro", "dr.georgescu", "doctor123", oftalmologie)
        ));
    }

    private Doctor doctor(String firstName, String lastName, String email,
                          String username, String rawPassword, Specialization spec) {
        Doctor d = new Doctor();
        d.setFirstName(firstName);
        d.setLastName(lastName);
        d.setEmail(email);
        d.setUsername(username);
        d.setPassword(passwordEncoder.encode(rawPassword));
        d.setSpecialization(spec);
        return d;
    }

    private void seedPatients() {
        if (patientRepo.count() > 0) return;

        patientRepo.saveAll(List.of(
                patient("Alex", "Mihai", "alex.mihai@example.com", "0740111222", "1995-04-12", "alex.mihai", "patient123"),
                patient("Diana", "Radu", "diana.radu@example.com", "0740333444", "1990-09-08", "diana.radu", "patient123"),
                patient("Mihai", "Constantin", "mihai.constantin@example.com", "0740555666", "1988-01-25", "mihai.constantin", "patient123"),
                patient("Ana", "Pop", "ana.pop@example.com", "0740777888", "2001-07-19", "ana.pop", "patient123")
        ));
    }

    private Patient patient(String firstName, String lastName, String email, String phone,
                            String dateOfBirth, String username, String rawPassword) {
        Patient p = new Patient();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setEmail(email);
        p.setPhone(phone);
        p.setDateOfBirth(dateOfBirth);
        p.setUsername(username);
        p.setPassword(passwordEncoder.encode(rawPassword));
        return p;
    }

    private void seedAppointments() {
        if (appointmentRepo.count() > 0) return;

        List<Patient> patients = patientRepo.findAll();
        List<Doctor> doctors = doctorRepo.findAll();
        if (patients.isEmpty() || doctors.isEmpty()) return;

        LocalDateTime base = LocalDateTime.now()
                .plusDays(2)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);

        appointmentRepo.saveAll(List.of(
                appointment(patients.get(0), doctors.get(0), base, "Control cardiologic anual", AppointmentStatus.PENDING),
                appointment(patients.get(1), doctors.get(1), base.plusDays(1).withHour(10), "Consult dermatologic", AppointmentStatus.ACCEPTED),
                appointment(patients.get(2 % patients.size()), doctors.get(2 % doctors.size()), base.plusDays(2).withHour(11), "Vaccinare copil", AppointmentStatus.PENDING)
        ));
    }

    private Appointment appointment(Patient p, Doctor d, LocalDateTime dt,
                                    String description, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setPatient(p);
        a.setDoctor(d);
        a.setAppointmentDatetime(dt);
        a.setDescription(description);
        a.setStatus(status);
        return a;
    }
}
