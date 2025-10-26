package com.example.demo.controller;

import com.example.demo.model.UserModel;
import com.example.demo.model.dto.RegisterUserRequest;
import com.example.demo.model.dto.UpdateUserRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        UserModel newUser = userService.register(request);
        return ResponseEntity.ok(newUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        var userOpt = userRepository.findByEmail(email);
        System.out.println(userOpt);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        var user = userOpt.get();

        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "firstName", user.getName(),
                "email", user.getEmail()));
    }

    @PutMapping("/update-user")
    public ResponseEntity<?> updateUser(
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // 1. Check if Authorization header exists and has Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid token"));
        }

        // 2. Extract token from "Bearer <token>"
        String token = authHeader.substring(7);

        // 3. Validate token and extract email
        String email;
        try {
            email = jwtService.extractUsername(token);
            if (email == null || !jwtService.isTokenValid(token, email)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        // 4. Check if user exists in database
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        // 5. Check if new email already exists (if email is being changed)
        if (!email.equals(request.getEmail())) {
            var existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
        }

        // 6. Update user in database
        UserModel user = userOpt.get();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        // 7. Return updated user
        return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }
}
