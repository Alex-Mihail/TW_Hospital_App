package com.example.proiecttw.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "doctors", indexes = {
        @Index(name = "idx_doctor_username", columnList = "username", unique = true),
        @Index(name = "idx_doctor_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(unique = true, length = 120)
    private String email;

    @ManyToOne
    @JoinColumn(name = "specialization_id", nullable = true)
    private Specialization specialization;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<Appointment> appointments;

    @Column(unique = true, nullable = false, length = 60)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 200)
    private String password;
}
