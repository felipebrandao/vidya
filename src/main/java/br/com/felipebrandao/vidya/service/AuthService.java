package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.dto.request.LoginRequest;
import br.com.felipebrandao.vidya.dto.request.RegisterRequest;
import br.com.felipebrandao.vidya.dto.response.AuthResponse;
import br.com.felipebrandao.vidya.entity.User;
import br.com.felipebrandao.vidya.repository.UserRepository;
import br.com.felipebrandao.vidya.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username já está em uso: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), jwtService.getExpiration());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), jwtService.getExpiration());
    }
}

