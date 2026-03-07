package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.dto.request.LoginRequest;
import br.com.felipebrandao.vidya.dto.request.RegisterRequest;
import br.com.felipebrandao.vidya.dto.response.AuthResponse;
import br.com.felipebrandao.vidya.entity.User;
import br.com.felipebrandao.vidya.exception.DuplicateResourceException;
import br.com.felipebrandao.vidya.exception.ResourceNotFoundException;
import br.com.felipebrandao.vidya.repository.UserRepository;
import br.com.felipebrandao.vidya.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register: deve criar usuário e retornar token quando username não existe")
    void givenNewUsernameWhenRegisterShouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("felipe", "secret123");
        Instant expiresAt = Instant.now().plusSeconds(3600);

        when(userRepository.existsByUsername("felipe")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("felipe")).thenReturn("jwt-token");
        when(jwtService.getExpirationInstant()).thenReturn(expiresAt);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("felipe");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);

        verify(userRepository).existsByUsername("felipe");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken("felipe");
    }

    @Test
    @DisplayName("register: deve lançar DuplicateResourceException quando username já existe")
    void givenExistingUsernameWhenRegisterShouldThrowDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("felipe", "secret123");

        when(userRepository.existsByUsername("felipe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("felipe");

        verify(userRepository).existsByUsername("felipe");
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("register: deve armazenar a senha codificada, não a senha em texto puro")
    void givenRegisterRequestWhenRegisterShouldEncodePassword() {
        RegisterRequest request = new RegisterRequest("user1", "plaintext");

        when(userRepository.existsByUsername("user1")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("bcrypt_hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("bcrypt_hash");
            return u;
        });
        when(jwtService.generateToken(anyString())).thenReturn("token");
        when(jwtService.getExpirationInstant()).thenReturn(Instant.now());

        authService.register(request);

        verify(passwordEncoder).encode("plaintext");
    }

    @Test
    @DisplayName("login: deve autenticar e retornar token quando credenciais são válidas")
    void givenValidCredentialsWhenLoginShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest("felipe", "secret123");
        User user = User.builder().username("felipe").password("hashed").build();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        when(userRepository.findByUsername("felipe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("felipe")).thenReturn("jwt-token");
        when(jwtService.getExpirationInstant()).thenReturn(expiresAt);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("felipe");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("felipe");
        verify(jwtService).generateToken("felipe");
    }

    @Test
    @DisplayName("login: deve lançar ResourceNotFoundException quando usuário não existe no repositório")
    void givenUnknownUsernameWhenLoginShouldThrowResourceNotFoundException() {
        LoginRequest request = new LoginRequest("unknown", "secret123");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("login: deve propagar exceção do AuthenticationManager quando credenciais são inválidas")
    void givenBadPasswordWhenLoginShouldPropagateAuthenticationException() {
        LoginRequest request = new LoginRequest("felipe", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("login: deve delegar autenticação ao AuthenticationManager com o token correto")
    void givenLoginRequestWhenLoginShouldDelegateToAuthenticationManager() {
        LoginRequest request = new LoginRequest("felipe", "secret123");
        User user = User.builder().username("felipe").password("hashed").build();

        when(userRepository.findByUsername("felipe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyString())).thenReturn("token");
        when(jwtService.getExpirationInstant()).thenReturn(Instant.now());

        authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("felipe", "secret123"));
    }
}



