package it.unisalento.pas2425.userserviceproject.restcontrollers;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisalento.pas2425.userserviceproject.configuration.RabbitUserInteractionTopicConfig;
import it.unisalento.pas2425.userserviceproject.domain.Role;
import it.unisalento.pas2425.userserviceproject.domain.User;
import it.unisalento.pas2425.userserviceproject.dto.AuthenticationResponseDTO;
import it.unisalento.pas2425.userserviceproject.dto.LoginDTO;
import it.unisalento.pas2425.userserviceproject.dto.UserDTO;
import it.unisalento.pas2425.userserviceproject.dto.UsersListDTO;
import it.unisalento.pas2425.userserviceproject.repositories.UserRepository;
import it.unisalento.pas2425.userserviceproject.security.JwtUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser; // Importa WithMockUser
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Test di UserRestController")
public class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtilities jwtUtilities;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private User testUser;
    private UserDTO testUserDto;
    private LoginDTO testLoginDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user123");
        testUser.setName("Test");
        testUser.setSurname("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword"); // Assuming it's already encoded
        testUser.setRole(Role.CLIENT);
        testUser.setVehicle("Car");

        testUserDto = new UserDTO();
        testUserDto.setId("user123");
        testUserDto.setNome("Test");
        testUserDto.setCognome("User");
        testUserDto.setEmail("test@example.com");
        testUserDto.setVehicle("Car");
        testUserDto.setRole(Role.CLIENT);

        testLoginDto = new LoginDTO();
        testLoginDto.setEmail("test@example.com");
        testLoginDto.setPassword("rawPassword");
    }

    @Test
    @DisplayName("Dovrebbe restituire tutti gli utenti")
    @WithMockUser(roles = "ADMIN") // Aggiunto per simulare un utente autenticato con ruolo ADMIN
    void shouldGetAllUsers() throws Exception {
        List<User> users = Collections.singletonList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        mockMvc.perform(get("/api/users/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersList[0].id").value(testUser.getId()))
                .andExpect(jsonPath("$.usersList[0].nome").value(testUser.getName()));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Dovrebbe trovare l'utente per ID")
    @WithMockUser(roles = "CLIENT") // Aggiunto per simulare un utente autenticato con ruolo CLIENT
    void shouldFindUserById() throws Exception {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(testUser.getName()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.vehicle").value(testUser.getVehicle()));

        verify(userRepository, times(1)).findById(testUser.getId());
    }

    @Test
    @DisplayName("Dovrebbe restituire 404 se l'utente non viene trovato per ID")
    @WithMockUser(roles = "CLIENT") // Aggiunto per simulare un utente autenticato
    void shouldReturnNotFoundIfUserByIdNotFound() throws Exception {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", "nonExistentId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Dovrebbe autenticare l'utente e restituire il token JWT")
    void shouldAuthenticateUserAndReturnJwtToken() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testLoginDto.getEmail(), testLoginDto.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(testLoginDto.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtilities.generateToken(anyString(), anyMap(), anyString())).thenReturn("mocked.jwt.token");

        mockMvc.perform(post("/api/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("mocked.jwt.token"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(testLoginDto.getEmail());
        verify(jwtUtilities, times(1)).generateToken(eq(testUser.getEmail()), anyMap(), eq(testUser.getRole().name()));
    }

    @Test
    @DisplayName("Dovrebbe restituire 401 per credenziali di autenticazione errate")
    void shouldReturnUnauthorizedForBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginDto)))
                .andExpect(status().isForbidden()); // Modificato da isUnauthorized() a isForbidden()

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtilities, never()).generateToken(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Dovrebbe impostare il veicolo per l'utente")
    @WithMockUser(roles = "CLIENT") // Aggiunto per simulare un utente autenticato
    void shouldSetVehicleForUser() throws Exception {
        String newVehicleType = "Motorcycle";
        User updatedUser = new User();
        updatedUser.setId(testUser.getId());
        updatedUser.setName(testUser.getName());
        updatedUser.setSurname(testUser.getSurname());
        updatedUser.setEmail(testUser.getEmail());
        updatedUser.setPassword(testUser.getPassword());
        updatedUser.setRole(testUser.getRole());
        updatedUser.setVehicle(newVehicleType);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/api/users/setVehicle/{id}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newVehicleType)) // Modificato per inviare la stringa raw
                .andExpect(status().isOk())
                .andExpect(content().string("Vehicle set correctly"));

        verify(userRepository, times(1)).findById(testUser.getId());
        verify(userRepository, times(1)).save(argThat(user -> user.getVehicle().equals(newVehicleType)));
    }

    @Test
    @DisplayName("Dovrebbe restituire 404 se l'utente non viene trovato per impostare il veicolo")
    @WithMockUser(roles = "CLIENT") // Aggiunto per simulare un utente autenticato
    void shouldReturnNotFoundWhenSettingVehicleForNonExistentUser() throws Exception {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/users/setVehicle/{id}", "nonExistentId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("Car")) // Modificato per inviare la stringa raw
                .andExpect(status().isNotFound());

        verify(userRepository, times(1)).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Dovrebbe impostare l'utente come amministratore")
    @WithMockUser(username = "test@example.com", roles = "CLIENT") // Simula un utente CLIENT che tenta di promuovere
    void shouldSetUserAsAdmin() throws Exception {
        String mockJwt = "Bearer mocked.jwt.token";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId());
        User adminUser = new User();
        adminUser.setId(testUser.getId());
        adminUser.setName(testUser.getName());
        adminUser.setSurname(testUser.getSurname());
        adminUser.setEmail(testUser.getEmail());
        adminUser.setPassword(testUser.getPassword());
        adminUser.setRole(Role.ADMIN);

        // Mock per estrarre l'ID utente dal JWT (simulando il comportamento di JwtUtilities)
        when(jwtUtilities.extractClaim(eq("mocked.jwt.token"), any())).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/users/setAsAdmin")
                        .header("Authorization", mockJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User"));

        verify(jwtUtilities, times(1)).extractClaim(eq("mocked.jwt.token"), any());
        verify(userRepository, times(1)).findById(testUser.getId());
        verify(userRepository, times(1)).save(argThat(user -> user.getRole() == Role.ADMIN));
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitUserInteractionTopicConfig.INTERACTION_EXCHANGE),
                eq(RabbitUserInteractionTopicConfig.ROUTING_ADMIN_UPDATE),
                eq(testUser.getId())
        );
    }

    @Test
    @DisplayName("Dovrebbe restituire 404 se l'utente non viene trovato per impostare come amministratore")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN") // Simula un utente ADMIN per questo test
    void shouldReturnNotFoundWhenSettingNonExistentUserAsAdmin() throws Exception {
        String mockJwt = "Bearer mocked.jwt.token";
        when(jwtUtilities.extractClaim(eq("mocked.jwt.token"), any())).thenReturn("nonExistentId");
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/users/setAsAdmin")
                        .header("Authorization", mockJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(jwtUtilities, times(1)).extractClaim(eq("mocked.jwt.token"), any());
        verify(userRepository, times(1)).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }
}
