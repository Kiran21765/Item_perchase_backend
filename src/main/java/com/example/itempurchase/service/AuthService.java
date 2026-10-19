package com.example.itempurchase.service;

import com.example.itempurchase.dto.Responses.AuthResponse;
import com.example.itempurchase.dto.Requests.LoginRequest;
import com.example.itempurchase.dto.Requests.RegisterRequest;
import com.example.itempurchase.entity.AppUser;
import com.example.itempurchase.entity.Role;
import com.example.itempurchase.exception.ApiException;
import com.example.itempurchase.repository.AppUserRepository;
import com.example.itempurchase.security.JwtUtil;
import com.example.itempurchase.util.Validators;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, AppUserRepository users,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null || Validators.isBlank(req.username()) || Validators.isBlank(req.password())) {
            throw ApiException.badRequest("Username and password are required");
        }
        // throws BadCredentialsException (-> 401) when the password is wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username().trim(), req.password()));
        AppUser user = users.findByUsername(req.username().trim())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return new AuthResponse(jwtUtil.generate(user.getUsername(), user.getRole().name()),
                user.getUsername(), user.getRole().name());
    }

    /** Public registration always creates a normal USER (never an ADMIN). */
    public AuthResponse register(RegisterRequest req) {
        if (req == null || Validators.isBlank(req.username()) || Validators.isBlank(req.password())) {
            throw ApiException.badRequest("Username and password are required");
        }
        String username = req.username().trim();
        if (!username.matches("^[A-Za-z0-9_.-]{3,50}$")) {
            throw ApiException.badRequest(
                    "Username must be 3-50 characters: letters, numbers, '.', '_' or '-'");
        }
        if (req.password().length() < 6 || req.password().length() > 72) {
            throw ApiException.badRequest("Password must be 6 to 72 characters");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Username already exists");
        }
        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(req.password()), Role.USER));
        return new AuthResponse(jwtUtil.generate(user.getUsername(), user.getRole().name()),
                user.getUsername(), user.getRole().name());
    }
}
