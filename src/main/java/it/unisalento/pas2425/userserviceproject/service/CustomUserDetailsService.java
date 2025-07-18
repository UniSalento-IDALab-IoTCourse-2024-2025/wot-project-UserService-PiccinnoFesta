package it.unisalento.pas2425.userserviceproject.service;

import it.unisalento.pas2425.userserviceproject.domain.Doctor;
import it.unisalento.pas2425.userserviceproject.repositories.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    DoctorRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Doctor> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()) {
            throw new UsernameNotFoundException(email);
        }

        Doctor user = userOptional.get();


        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .build();

        return userDetails;
    }
}
