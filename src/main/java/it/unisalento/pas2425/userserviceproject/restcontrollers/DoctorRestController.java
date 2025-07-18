package it.unisalento.pas2425.userserviceproject.restcontrollers;


import it.unisalento.pas2425.userserviceproject.domain.Doctor;
import it.unisalento.pas2425.userserviceproject.domain.Patient;
import it.unisalento.pas2425.userserviceproject.dto.*;
import it.unisalento.pas2425.userserviceproject.exceptions.UserNotFoundException;
import it.unisalento.pas2425.userserviceproject.repositories.DoctorRepository;
import it.unisalento.pas2425.userserviceproject.repositories.PatientRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;


import java.util.*;

@RestController
@RequestMapping("/api/doctor")
public class DoctorRestController {


    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtilities jwtUtilities;


    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    private PatientRepository patientRepository;



    @RequestMapping(value="/getProfile",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String jwtToken) throws UserNotFoundException{



        // 1. Estrai il JWT e il doctorId
        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        // 2. Recupera il Doctor
        Doctor doctor = doctorRepository.findById(doctorIdFromToken)
                .orElseThrow(() -> new RuntimeException("Doctor non trovato: " + doctorIdFromToken));

        DoctorDTO doctorDTO = new DoctorDTO();
        doctorDTO.setId(doctor.getId());
        doctorDTO.setName(doctor.getName());
        doctorDTO.setSurname(doctor.getSurname());
        doctorDTO.setEmail(doctor.getEmail());
        doctorDTO.setLicense(doctor.getLicense());


        return ResponseEntity.ok(doctorDTO);
    }








}
