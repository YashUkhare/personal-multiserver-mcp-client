package com.mcp.client.controller;

import com.mcp.client.entity.UserEntity;
import com.mcp.client.repository.UserRepository;
import com.mcp.client.security.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager, JwtService jwtService,
            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam(defaultValue = "jimmy") String username,
            @RequestParam(defaultValue = "jimmy@123") String password) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        String role = auth.getAuthorities().iterator().next().getAuthority();
        String token = jwtService.generateToken(username, role);

        return ResponseEntity.ok(Map.of(
                "username", username,
                "role", role,
                "token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.getOrDefault("role", "USER"); // default role if not provided

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already exists"));
        }

        String encodedPassword = passwordEncoder.encode(password);

        UserEntity user = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .role(role.toUpperCase())
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "username", username,
                "role", role));
    }

}
