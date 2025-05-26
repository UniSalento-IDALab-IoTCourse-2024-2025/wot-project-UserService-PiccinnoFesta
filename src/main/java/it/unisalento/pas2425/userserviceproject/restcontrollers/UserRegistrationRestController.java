package it.unisalento.pas2425.userserviceproject.restcontrollers;

import it.unisalento.pas2425.userserviceproject.di.IPaymentService;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.dto.RegistrationResultDTO;
import it.unisalento.pas2425.userserviceproject.dto.UserDTO;
import it.unisalento.pas2425.userserviceproject.dto.UsersListDTO;
import it.unisalento.pas2425.userserviceproject.exceptions.UserNotFoundException;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.unisalento.pas2425.userserviceproject.configuration.SecurityConfig.passwordEncoder;

@RestController
@RequestMapping("/api/registration")
public class UserRegistrationRestController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @RequestMapping(value="/",
                    method=RequestMethod.POST,
                    produces = MediaType.APPLICATION_JSON_VALUE,
                    consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public RegistrationResultDTO save(@RequestBody UserDTO userDto) {

        RegistrationResultDTO resultDTO = new RegistrationResultDTO();

        Optional<User> existingUser = userRepository.findByEmail(userDto.getEmail());

        if(!existingUser.isEmpty()){
            resultDTO.setResult(RegistrationResultDTO.EMAIL_ALREADY_EXIST);
            resultDTO.setMessage("La mail è già associata ad un altro utente");
            return resultDTO;
        }

        User user = new User();
        user.setName(userDto.getNome());
        user.setSurname(userDto.getCognome());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder().encode(userDto.getPassword()));
        user.setRole(userDto.getRuolo());

        //inviare messaggio a wallet per creare un wallet

        user = userRepository.save(user); //giochetto serve per generare id e salvarlo
        userDto.setId(user.getId());


        resultDTO.setResult(RegistrationResultDTO.OK);
        resultDTO.setUser(userDto);

        return resultDTO;
    }


}
