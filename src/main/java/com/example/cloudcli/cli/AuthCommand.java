package com.example.cloudcli.cli;

import com.example.cloudcli.model.User;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Authentication commands for user management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCommand {
    
    private final UserService userService;
    private final SessionManager sessionManager;
    private final Scanner scanner = new Scanner(System.in);
    
    @Command(name = "register", description = "Register a new user")
    @Component
    @RequiredArgsConstructor
    public static class RegisterCommand implements Callable<Integer> {
        
        private final UserService userService;
        private final SessionManager sessionManager;
        
        @Option(names = {"-e", "--email"}, description = "Email address", required = true)
        private String email;
        
        @Option(names = {"-u", "--username"}, description = "Username", required = true)
        private String username;
        
        @Option(names = {"-p", "--password"}, description = "Password", required = true)
        private String password;
        
        @Override
        public Integer call() {
            try {
                User user = userService.register(email, username, password);
                System.out.println("✅ Registration successful! Welcome, " + user.getUsername() + "! 🎉");
                System.out.println("   Email: " + user.getEmail());
                System.out.println("   User ID: " + user.getUserId());
                System.out.println("");
                
                // Auto-login after registration
                sessionManager.login(user);
                System.out.println("🔐 You are now logged in as: " + user.getUsername());
                System.out.println("   Run ./cloudcli to start using CloudCLI");
                return 0;
            } catch (IllegalArgumentException e) {
                System.err.println("❌ Registration failed: " + e.getMessage());
                return 1;
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                log.error("Registration error", e);
                return 1;
            }
        }
    }
    
    @Command(name = "login", description = "Login to your account")
    @Component
    @RequiredArgsConstructor
    public static class LoginCommand implements Callable<Integer> {
        
        private final UserService userService;
        private final SessionManager sessionManager;
        
        @Option(names = {"-u", "--username"}, description = "Username or email", required = true)
        private String usernameOrEmail;
        
        @Option(names = {"-p", "--password"}, description = "Password", required = true)
        private String password;
        
        @Override
        public Integer call() {
            try {
                Optional<User> userOpt = userService.login(usernameOrEmail, password);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    sessionManager.login(user);
                    System.out.println("✅ Login successful!");
                    System.out.println("   Welcome back, " + user.getUsername() + "!");
                    System.out.println("   User ID: " + user.getUserId());
                    return 0;
                } else {
                    System.err.println("❌ Login failed: Invalid username/email or password");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                log.error("Login error", e);
                return 1;
            }
        }
    }
    
    @Command(name = "logout", description = "Logout from your account")
    @Component
    @RequiredArgsConstructor
    public static class LogoutCommand implements Callable<Integer> {
        
        private final SessionManager sessionManager;
        
        @Override
        public Integer call() {
            if (sessionManager.isLoggedIn()) {
                String username = sessionManager.getCurrentUsername();
                sessionManager.logout();
                System.out.println("✅ Logged out successfully!");
                System.out.println("   Goodbye, " + username + "!");
                return 0;
            } else {
                System.out.println("ℹ️  You are not logged in");
                return 0;
            }
        }
    }
    
    @Command(name = "whoami", description = "Show current user")
    @Component
    @RequiredArgsConstructor
    public static class WhoAmICommand implements Callable<Integer> {
        
        private final SessionManager sessionManager;
        
        @Override
        public Integer call() {
            if (sessionManager.isLoggedIn()) {
                Optional<User> userOpt = sessionManager.getCurrentUser();
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    System.out.println("Current user:");
                    System.out.println("   Username: " + user.getUsername());
                    System.out.println("   Email: " + user.getEmail());
                    System.out.println("   User ID: " + user.getUserId());
                    return 0;
                }
            }
            System.out.println("Not logged in");
            return 0;
        }
    }
}
