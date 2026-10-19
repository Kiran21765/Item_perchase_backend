package com.example.itempurchase.controller;

import com.example.itempurchase.dto.Responses.AuthResponse;
import com.example.itempurchase.dto.Requests.LoginRequest;
import com.example.itempurchase.dto.Requests.RegisterRequest;
import com.example.itempurchase.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return Map.of("username", authentication.getName(), "role", role);
    }
}
