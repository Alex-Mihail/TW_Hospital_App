package com.example.proiecttw.hospital.repository;

import com.example.proiecttw.hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialization_NameIgnoreCase(String specializationName);

    Optional<Doctor> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Doctor> findBySpecialization_Id(Long specializationId);
}
