package com.tms.backend.auth;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.tms.backend.dto.LoginDTO;
import com.tms.backend.jwt.JwtService;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginDTO login(String identifier, String password) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }

        // Resolve user by email or username
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT
        String token = jwtService.generateToken(user);

        // Build LoginDTO response
        return new LoginDTO(
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.isVerified(),
            user.isProfileComplete(),
            user.getRole().getId(),
            user.getRole().getName(),
            token,
            user.isActive(),
            user.getUsername(),
            user.getUid()
        );
    }
}
