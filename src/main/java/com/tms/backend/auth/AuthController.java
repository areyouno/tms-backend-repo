package com.tms.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.LoginDTO;
import com.tms.backend.dto.LoginRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginDTO> login(@RequestBody LoginRequest request) {
        LoginDTO response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }
}
