package it.unisalento.pas2425.userserviceproject.restcontrollers;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisalento.pas2425.userserviceproject.configuration.RabbitUserInteractionTopicConfig;
import it.unisalento.pas2425.userserviceproject.domain.Role;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.dto.RegistrationResultDTO;
import it.unisalento.pas2425.userserviceproject.dto.UserDTO;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Test di UserRegistrationRestController")
public class UserRegistrationRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private UserDTO newUserDto;
    private User savedUser;

    @BeforeEach
    void setUp() {
        newUserDto = new UserDTO();
        newUserDto.setNome("John");
        newUserDto.setCognome("Doe");
        newUserDto.setEmail("john.doe@example.com");
        newUserDto.setPassword("password123");

        savedUser = new User();
        savedUser.setId("test-user-id-123");
        savedUser.setName("John");
        savedUser.setSurname("Doe");
        savedUser.setEmail("john.doe@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.CLIENT);
    }

    @Test
    @DisplayName("Dovrebbe registrare con successo un nuovo utente")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        when(userRepository.findByEmail(newUserDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/registration/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.result").value(RegistrationResultDTO.OK))
                .andExpect(jsonPath("$.user.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.user.nome").value(newUserDto.getNome()))
                .andExpect(jsonPath("$.user.email").value(newUserDto.getEmail()));

        verify(userRepository).findByEmail(newUserDto.getEmail());
        verify(userRepository).save(any(User.class));
        verify(rabbitTemplate).convertAndSend(
                RabbitUserInteractionTopicConfig.INTERACTION_EXCHANGE,
                RabbitUserInteractionTopicConfig.ROUTING_CREATE_WALLET,
                savedUser.getId()
        );
    }

    @Test
    @DisplayName("Dovrebbe restituire bad request se l'email esiste già")
    void shouldReturnBadRequestIfEmailAlreadyExists() throws Exception {
        when(userRepository.findByEmail(newUserDto.getEmail())).thenReturn(Optional.of(savedUser));

        mockMvc.perform(post("/api/registration/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.result").value(RegistrationResultDTO.EMAIL_ALREADY_EXIST))
                .andExpect(jsonPath("$.message").value("La mail è già associata ad un altro utente"));

        verify(userRepository).findByEmail(newUserDto.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Dovrebbe restituire bad request per input JSON malformato")
    void shouldReturnBadRequestForMalformedJsonInput() throws Exception {
        String malformedJson = "{\"nome\":\"Test\",\"cognome\":\"User\",\"email\":\"test@example.com\",\"password\":\"pass\"";

        mockMvc.perform(post("/api/registration/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }
}
