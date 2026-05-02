package com.example.cloudcli.service;

import com.example.cloudcli.model.User;
import com.example.cloudcli.repository.UserRepository;
import com.example.cloudcli.service.notification.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * User management service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final Optional<WelcomeEmailService> welcomeEmailService;
    
    /**
     * Register a new user
     */
    public User register(String email, String username, String password) {
        // Validate
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        
        // Hash password
        String passwordHash = hashPassword(password);
        
        // Create user
        User user = User.create(email, username, passwordHash);
        userRepository.save(user);
        
        log.info("User registered: {} ({})", username, email);
        
        // Send welcome email
        welcomeEmailService.ifPresent(service -> {
            try {
                service.sendWelcomeEmail(user);
                log.info("Welcome email sent to new user: {}", email);
            } catch (Exception e) {
                log.error("Failed to send welcome email to: {}", email, e);
            }
        });
        
        return user;
    }
    
    /**
     * Authenticate user
     */
    public Optional<User> login(String usernameOrEmail, String password) {
        // Try to find by email first, then username
        Optional<User> userOpt = userRepository.findByEmail(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(usernameOrEmail);
        }
        
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found - {}", usernameOrEmail);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        String passwordHash = hashPassword(password);
        
        if (passwordHash.equals(user.getPasswordHash())) {
            log.info("User logged in: {}", user.getUsername());
            return Optional.of(user);
        } else {
            log.warn("Login failed: incorrect password - {}", usernameOrEmail);
            return Optional.empty();
        }
    }
    
    /**
     * Simple password hashing (SHA-256)
     * TODO: Use BCrypt or Argon2 in production
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
}
