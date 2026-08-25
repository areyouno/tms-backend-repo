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

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginHistoryService loginHistoryService;

    public AuthService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       LoginHistoryService loginHistoryService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginHistoryService = loginHistoryService;
    }

    public LoginDTO login(String identifier, String password, HttpServletRequest request) {
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

        return buildSession(user, request);
    }

    /**
     * Establishes an authenticated session for an already-resolved, trusted
     * user (no credential check) — used by the login flow above, and by the
     * set-password flow (the caller already proved ownership via a single-use
     * verification token).
     */
    public LoginDTO buildSession(User user, HttpServletRequest request) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        loginHistoryService.recordLogin(user, request);

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
            user.getUid(),
            user.getOrganizationName(),
            user.getCountry()
        );
    }
}
