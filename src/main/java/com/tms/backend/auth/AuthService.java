package com.tms.backend.auth;

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

    public LoginDTO login(String email, String password) {
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid email or password");
        }

        // Load user from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
            token
        );
    }
}
