package it.unisalento.pas2425.userserviceproject.restcontrollers;

import it.unisalento.pas2425.userserviceproject.configuration.RabbitUserInteractionTopicConfig;
import it.unisalento.pas2425.userserviceproject.di.IPaymentService;
import it.unisalento.pas2425.userserviceproject.domain.Role;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.dto.*;
import it.unisalento.pas2425.userserviceproject.exceptions.UserNotFoundException;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;


import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserRestController {


    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtilities jwtUtilities;


    @Autowired
    UserRepository userRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    IPaymentService creditCardPaymentService;

    @RequestMapping(value="/",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public UsersListDTO getAll() {



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

        Optional<User> user = userRepository.findById(id);

        if(user.isEmpty()){
            throw new UserNotFoundException();
        }

        UserDTO userDto = new UserDTO();

        //non dovrebbe esserci bisogno di ritornarlo
        //userDto.setId(user.get().getId());
        userDto.setNome(user.get().getName());
        userDto.setCognome(user.get().getSurname());
        userDto.setEmail(user.get().getEmail());
        userDto.setVehicle(user.get().getVehicle() );
        userDto.setRole(user.get().getRole());

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
        System.out.println(user.get().getRole().name());
        final String jwt = jwtUtilities.generateToken(user.get().getEmail(), claims,user.get().getRole().name() );
        return ResponseEntity.ok(new AuthenticationResponseDTO(jwt));
    }



    @RequestMapping(value="/setVehicle/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<?> setVehicle(@PathVariable String id,@RequestBody String vehicleType ) throws UserNotFoundException{
        Optional<User> optionalUser = userRepository.findById(id);
        if(optionalUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }


        User user = optionalUser.get();
        user.setVehicle(vehicleType);
        userRepository.save(user);

        return ResponseEntity.ok("Vehicle set correctly");
    }


    //backdoor per aggiungere un admin, da rimuovere poi in produzione
    @PostMapping(value = "/setAsAdmin")
    public ResponseEntity<String> setAsAdmin(@RequestHeader("Authorization") String jwtToken) throws UserNotFoundException {

        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String userIdFromToken= jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));;



        Optional<User> optionalUser = userRepository.findById(userIdFromToken);
        if(optionalUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        User user = optionalUser.get();
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        //faccio aggiornare l'id admin al wallet
        rabbitTemplate.convertAndSend(RabbitUserInteractionTopicConfig.INTERACTION_EXCHANGE
                ,RabbitUserInteractionTopicConfig.ROUTING_ADMIN_UPDATE,
                user.getId());
        return  ResponseEntity.ok().body("User");
    }







}
