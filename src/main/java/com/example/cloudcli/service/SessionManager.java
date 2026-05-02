package com.example.cloudcli.service;

import com.example.cloudcli.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Manages user session for CLI with file-based persistence
 * Session is stored in ~/.cloudcli/session.json
 */
@Slf4j
@Service
public class SessionManager {
    
    private static final String SESSION_DIR = System.getProperty("user.home") + "/.cloudcli";
    private static final String SESSION_FILE = SESSION_DIR + "/session.json";
    private final ObjectMapper objectMapper;
    
    private User currentUser;
    
    public SessionManager() {
        // Configure ObjectMapper with Java 8 date/time support
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Load session on startup
        loadSession();
    }
    
    public void login(User user) {
        this.currentUser = user;
        saveSession();
        log.info("Session started for user: {}", user.getUsername());
    }
    
    public void logout() {
        if (currentUser != null) {
            log.info("Session ended for user: {}", currentUser.getUsername());
            currentUser = null;
            deleteSession();
        }
    }
    
    public Optional<User> getCurrentUser() {
        if (currentUser == null) {
            loadSession();
        }
        return Optional.ofNullable(currentUser);
    }
    
    public boolean isLoggedIn() {
        if (currentUser == null) {
            loadSession();
        }
        return currentUser != null;
    }
    
    public String getCurrentUserId() {
        if (currentUser == null) {
            loadSession();
        }
        return currentUser != null ? currentUser.getUserId() : null;
    }
    
    public String getCurrentUsername() {
        if (currentUser == null) {
            loadSession();
        }
        return currentUser != null ? currentUser.getUsername() : "anonymous";
    }
    
    private void saveSession() {
        try {
            // Create directory if it doesn't exist
            Path sessionDir = Paths.get(SESSION_DIR);
            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }
            
            // Write session to file
            objectMapper.writeValue(new File(SESSION_FILE), currentUser);
            log.debug("Session saved to {}", SESSION_FILE);
        } catch (IOException e) {
            log.error("Failed to save session: {}", e.getMessage());
        }
    }
    
    private void loadSession() {
        try {
            File sessionFile = new File(SESSION_FILE);
            if (sessionFile.exists()) {
                currentUser = objectMapper.readValue(sessionFile, User.class);
                log.debug("Session loaded from {}", SESSION_FILE);
            }
        } catch (IOException e) {
            log.debug("No valid session found: {}", e.getMessage());
            currentUser = null;
        }
    }
    
    private void deleteSession() {
        try {
            File sessionFile = new File(SESSION_FILE);
            if (sessionFile.exists()) {
                Files.delete(sessionFile.toPath());
                log.debug("Session file deleted");
            }
        } catch (IOException e) {
            log.error("Failed to delete session file: {}", e.getMessage());
        }
    }
}
