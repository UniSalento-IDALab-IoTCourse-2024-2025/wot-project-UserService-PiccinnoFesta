package it.unisalento.pas2425.userserviceproject.restcontrollers;

import com.rabbitmq.client.Return;
import it.unisalento.pas2425.userserviceproject.configuration.RabbitAdminFanoutConfig;
import it.unisalento.pas2425.userserviceproject.domain.Role;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.springframework.amqp.rabbit.core.RabbitAdminEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private JwtUtilities jwtUtilities;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/changeAdmin/{id}")
    public ResponseEntity<String> changeAdmin(@PathVariable String newAdminId, @RequestHeader("Authorization") String jwtToken){

        String jwt = jwtToken.startsWith("Bearer ") ? jwtToken.substring(7) : jwtToken;
        String adminIdFromToken;

        //User non valido
        try {
            adminIdFromToken = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Jwt Token non valido");
        }
        if(userRepository.findById(newAdminId).isEmpty() || userRepository.findById(adminIdFromToken).isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Utente e/o Admin non trovato");
        }


        User oldAdmin = userRepository.findById(adminIdFromToken).get();
        oldAdmin.setRole(Role.CLIENT);

        User newAdmin = userRepository.findById(newAdminId).get();
        newAdmin.setRole(Role.ADMIN);

        userRepository.save(oldAdmin);
        userRepository.save(newAdmin);


        rabbitTemplate.convertAndSend(RabbitAdminFanoutConfig.ADMIN_UPDATE_EXCHANGE_NAME,"", newAdmin.getId());
        System.out.println("UserService: Published Admin User ID (as String): " + newAdmin.getId() + " on Admin Update Exchange");
        return ResponseEntity.ok("Admin cambiato con successo");
    }



}
