package it.unisalento.pas2425.userserviceproject.repositories;

import it.unisalento.pas2425.userserviceproject.domain.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DoctorRepository extends MongoRepository<Doctor, String> {

    Optional<Doctor> findByEmail(String email);


}
