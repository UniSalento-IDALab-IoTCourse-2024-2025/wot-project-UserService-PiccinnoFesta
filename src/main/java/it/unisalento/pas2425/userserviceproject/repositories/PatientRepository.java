package it.unisalento.pas2425.userserviceproject.repositories;

import it.unisalento.pas2425.userserviceproject.domain.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

}