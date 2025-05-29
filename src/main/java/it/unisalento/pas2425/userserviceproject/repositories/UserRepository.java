package it.unisalento.pas2425.userserviceproject.repositories;

import it.unisalento.pas2425.userserviceproject.domain.Role;
import it.unisalento.pas2425.userserviceproject.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByNameAndSurname(String nome, String surname);

    List<User> findByRole(Role role);
}
