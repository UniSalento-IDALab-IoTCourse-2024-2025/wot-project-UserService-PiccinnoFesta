package it.unisalento.pas2425.userserviceproject.restcontrollers;

import it.unisalento.pas2425.userserviceproject.configuration.RabbitConfig;
import it.unisalento.pas2425.userserviceproject.di.IPaymentService;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.dto.*;
import it.unisalento.pas2425.userserviceproject.exceptions.UserNotFoundException;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtilities jwtUtilities;


    @Autowired
    UserRepository userRepository;

    @Autowired
    IPaymentService creditCardPaymentService;

    @RequestMapping(value="/",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public UsersListDTO getAll() {

        creditCardPaymentService.initialize();
/*
        UserDTO userDto = new UserDTO();
        userDto.setId(111);
        userDto.setNome("Paolo");
        userDto.setCognome("Bianchi");
        userDto.setEmail("paolo.bianchi@gmail.com");
*/
        List<UserDTO> list = new ArrayList<UserDTO>();
        List<User> users = userRepository.findAll();

        for(User user: users){
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setNome(user.getName());
            userDTO.setCognome(user.getSurname());
            userDTO.setEmail(user.getEmail());

            list.add(userDTO);
        }


        UsersListDTO listDTO = new UsersListDTO();
        listDTO.setUsersList(list);

        return listDTO;
    }

    @RequestMapping(value="/{id}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserDTO findById(@PathVariable String id) throws UserNotFoundException{

        //mi evita ci sia un user = Null, ma anche se non lo trova per quell'id, istanzia qualcosa
        Optional<User> user = userRepository.findById(id);

        if(user.isEmpty()){
            throw new UserNotFoundException();
        }

        UserDTO userDto = new UserDTO();

        userDto.setId(user.get().getId());
        userDto.setNome(user.get().getName());
        userDto.setCognome(user.get().getSurname());
        userDto.setEmail(user.get().getEmail());

        //user.get()


        return userDto;
    }


    @RequestMapping(value="/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getPassword()
                )
        );
        Optional<User> user = userRepository.findByEmail(authentication.getName());
        if(user.isEmpty()) {
            throw new UsernameNotFoundException(loginDTO.getEmail());
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",user.get().getId());
        final String jwt = jwtUtilities.generateToken(user.get().getEmail(), claims);
        return ResponseEntity.ok(new AuthenticationResponseDTO(jwt));
    }


    @PostMapping("/codaSemplice")
    public String sendMessage(@RequestParam String message) {
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_NAME, message);
        return "Messaggio inviato: " + message;
    }

}
