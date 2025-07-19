package it.unisalento.pas2425.userserviceproject.restcontrollers;


import it.unisalento.pas2425.userserviceproject.domain.Doctor;
import it.unisalento.pas2425.userserviceproject.dto.AuthenticationResponseDTO;
import it.unisalento.pas2425.userserviceproject.dto.DoctorDTO;
import it.unisalento.pas2425.userserviceproject.dto.LoginDTO;
import it.unisalento.pas2425.userserviceproject.dto.RegistrationResultDTO;
import it.unisalento.pas2425.userserviceproject.exceptions.UserNotFoundException;
import it.unisalento.pas2425.userserviceproject.repositories.DoctorRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static it.unisalento.pas2425.userserviceproject.configuration.SecurityConfig.passwordEncoder;

@RestController
@RequestMapping("/api/registration")
public class UserRegistrationRestController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    DoctorRepository doctorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JwtUtilities jwtUtilities;


    @RequestMapping(value="/",
                    method=RequestMethod.POST,
                    produces = MediaType.APPLICATION_JSON_VALUE,
                    consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> save(@RequestBody DoctorDTO doctorDTO) {

        RegistrationResultDTO resultDTO = new RegistrationResultDTO();

        Optional<Doctor> existingDoctor = doctorRepository.findByEmail(doctorDTO.getEmail());

        if(!existingDoctor.isEmpty()){
            resultDTO.setResult(RegistrationResultDTO.EMAIL_ALREADY_EXIST);
            resultDTO.setMessage("La mail è già associata ad un altro utente");
            return ResponseEntity.badRequest().body(resultDTO);
        }

        Doctor doctor = new Doctor();
        doctor.setName(doctorDTO.getName());
        doctor.setSurname(doctorDTO.getSurname());
        doctor.setEmail(doctorDTO.getEmail());
        doctor.setPassword(passwordEncoder().encode(doctorDTO.getPassword()));
        doctor.setLicense(doctorDTO.getLicense());
        doctor.setPatientIds(new ArrayList<>());

        System.out.println("dentro la chiamata");

        doctor = doctorRepository.save(doctor); //giochetto serve per generare id e salvarlo
        doctorDTO.setId(doctor.getId());



        resultDTO.setResult(RegistrationResultDTO.OK);
        resultDTO.setUser(doctorDTO);

        return ResponseEntity.ok(resultDTO);
    }

    @RequestMapping(value="/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getPassword()
                )
        );
        Optional<Doctor> doctor = doctorRepository.findByEmail(authentication.getName());
        if(doctor.isEmpty()) {
            throw new UsernameNotFoundException(loginDTO.getEmail());
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",doctor.get().getId());
        final String jwt = jwtUtilities.generateToken(doctor.get().getEmail(), claims );
        return ResponseEntity.ok(new AuthenticationResponseDTO(jwt));
    }

}
