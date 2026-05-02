package com.example.cloudcli.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity for multi-tenant backup system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_username", columnList = "username", unique = true)
})
public class User {
    
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    public static User create(String email, String username, String passwordHash) {
        return User.builder()
            .userId(UUID.randomUUID().toString())
            .email(email)
            .username(username)
            .passwordHash(passwordHash)
            .createdAt(Instant.now())
            .build();
    }
}
