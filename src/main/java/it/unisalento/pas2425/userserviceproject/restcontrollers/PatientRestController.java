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
@RequestMapping("/api/patients")
public class PatientRestController {


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


    @RequestMapping(value="/",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public PatientListDTO getAll(@RequestHeader("Authorization") String jwtToken) {

        // 1. Estrai il JWT e il doctorId
        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        // 2. Recupera il Doctor
        Doctor doctor = doctorRepository.findById(doctorIdFromToken)
                .orElseThrow(() -> new RuntimeException("Doctor non trovato: " + doctorIdFromToken));

        // 3. Prendi la lista di ID pazienti associati (parkingsIds)
        List<String> patientIds = doctor.getPatientIds();

        // 4. Recupera solo i pazienti i cui ID sono contenuti nella lista
        List<Patient> patients = patientRepository.findAllById(patientIds);

        List<PatientDTO> list = new ArrayList<>();


        for(Patient patient: patients){
            PatientDTO patientDTO = new PatientDTO();
            patientDTO.setId(patient.getId());
            patientDTO.setName(patient.getName());
            patientDTO.setSurname(patient.getSurname());
            patientDTO.setGender(patient.getGender());
            patientDTO.setAge(patient.getAge());
            patientDTO.setTraits(patient.getTraits());
            patientDTO.setHeight(patient.getHeight());
            patientDTO.setWeight(patient.getWeight());
            patientDTO.setDiagnosis(patient.getDiagnosis());

            list.add(patientDTO);
        }


        PatientListDTO listDTO = new PatientListDTO();
        listDTO.setPatientList(list);

        return listDTO;
    }

    @RequestMapping(value="/{id}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> findById(@PathVariable String id,@RequestHeader("Authorization") String jwtToken) throws UserNotFoundException{

        Optional<Patient> optionalPatient = patientRepository.findById(id);

        if(optionalPatient.isEmpty()){
            throw new UserNotFoundException();
        }

        Patient patient = optionalPatient.get();


        // 1. Estrai il JWT e il doctorId
        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        // 2. Recupera il Doctor
        Doctor doctor = doctorRepository.findById(doctorIdFromToken)
                .orElseThrow(() -> new RuntimeException("Doctor non trovato: " + doctorIdFromToken));


        // 3. Verifica che il paziente sia associato al doctor
        if (!doctor.getPatientIds().contains(patient.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setId(patient.getId());
        patientDTO.setName(patient.getName());
        patientDTO.setSurname(patient.getSurname());
        patientDTO.setGender(patient.getGender());
        patientDTO.setAge(patient.getAge());
        patientDTO.setTraits(patient.getTraits());
        patientDTO.setHeight(patient.getHeight());
        patientDTO.setWeight(patient.getWeight());
        patientDTO.setDiagnosis(patient.getDiagnosis());


        return ResponseEntity.ok(patientDTO);
    }

    @RequestMapping(value="/insert",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> insert(@RequestBody PatientDTO patientDTO,@RequestHeader("Authorization") String jwtToken) throws UserNotFoundException {

        Patient patient = new Patient();

        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        Optional<Doctor> optionalDoctor = doctorRepository.findById(doctorIdFromToken);

        if(optionalDoctor.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Doctor doctor = optionalDoctor.get();

        patient.setName(patientDTO.getName());
        patient.setSurname(patientDTO.getSurname());
        patient.setAge(patientDTO.getAge());
        patient.setDiagnosis(patientDTO.getDiagnosis());
        patient.setHeight(patientDTO.getHeight());
        patient.setWeight(patientDTO.getWeight());
        patient.setGender(patientDTO.getGender());
        patient.setTraits(patientDTO.getTraits());

        patient = patientRepository.save(patient);
        patientDTO.setId(patient.getId()); //gioco per generare l'id e salvarlo


        doctor.getPatientIds().add(patient.getId());

        doctorRepository.save(doctor);


        return ResponseEntity.ok(patientDTO);
    }



    @RequestMapping(
            value = "/{patientId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> delete(
            @PathVariable("patientId") String patientId,
            @RequestHeader("Authorization") String jwtToken) {

        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        Optional<Doctor> optionalDoctor = doctorRepository.findById(doctorIdFromToken);
        if (optionalDoctor.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor non trovato");
        }
        Doctor doctor = optionalDoctor.get();

        if (!doctor.getPatientIds().contains(patientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Paziente non associato a questo medico");
        }

        // 4. Controlla se il paziente esiste
        Optional<Patient> patient = patientRepository.findById(patientId);
        if (patient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paziente non trovato");
        }

        patientRepository.deleteById(patientId);
        doctor.getPatientIds().remove(patientId);

        return ResponseEntity.ok().body("Paziente eliminato con successo");
    }


    @RequestMapping(
            value = "/update",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> update(
            @RequestBody PatientDTO patientDTO,
            @RequestHeader("Authorization") String jwtToken) {

        // 1. Estrai il JWT e il doctorId
        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String doctorIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));

        // 2. Recupera il Doctor
        Optional<Doctor> optionalDoctor = doctorRepository.findById(doctorIdFromToken);
        if (optionalDoctor.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor non trovato");
        }
        Doctor doctor = optionalDoctor.get();

        // 3. Controlla che patientDTO contenga un ID
        if (patientDTO.getId() == null || patientDTO.getId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID del paziente mancante");
        }

        // 4. Recupera il paziente esistente
        Optional<Patient> optionalPatient = patientRepository.findById(patientDTO.getId());
        if (optionalPatient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paziente non trovato");
        }

        Patient patient = optionalPatient.get();

        // 5. Verifica che il paziente sia associato al doctor
        if (!doctor.getPatientIds().contains(patient.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Paziente non associato a questo medico");
        }

        // 6. Aggiorna i campi del paziente
        patient.setName(patientDTO.getName());
        patient.setSurname(patientDTO.getSurname());
        patient.setAge(patientDTO.getAge());
        patient.setDiagnosis(patientDTO.getDiagnosis());
        patient.setHeight(patientDTO.getHeight());
        patient.setWeight(patientDTO.getWeight());
        patient.setGender(patientDTO.getGender());
        patient.setTraits(patientDTO.getTraits());

        // 7. Salva e restituisci il DTO aggiornato
        patient = patientRepository.save(patient);

        return ResponseEntity.ok(patientDTO);
    }

















}
