package com.example.cloudcli.cli;

import com.example.cloudcli.model.User;
import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Enhanced Interactive Shell with JLine 3
 * Features: Auto-completion, command history, colored output, progress bars
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "shell",
    description = "Launch enhanced interactive shell",
    mixinStandardHelpOptions = true
)
public class EnhancedInteractiveShell implements Callable<Integer> {
    
    private final SessionManager sessionManager;
    private final UserService userService;
    private final InMemoryBackupRepository backupRepository;
    
    private Terminal terminal;
    private LineReader lineReader;
    private boolean running = true;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());
    
    @Override
    public Integer call() {
        try {
            initializeTerminal();
            showWelcomeBanner();
            
            // Check if user is already logged in
            if (sessionManager.isLoggedIn()) {
                showLoggedInMessage();
            } else {
                showLoginPrompt();
            }
            
            // Main shell loop
            while (running) {
                try {
                    String prompt = buildPrompt();
                    String line = lineReader.readLine(prompt);
                    
                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    processCommand(line.trim());
                    
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed
                    println("\n👋 Use 'exit' to quit");
                } catch (EndOfFileException e) {
                    // Ctrl+D pressed
                    running = false;
                }
            }
            
            showGoodbyeMessage();
            return 0;
            
        } catch (Exception e) {
            log.error("Shell error", e);
            System.err.println("❌ Shell error: " + e.getMessage());
            return 1;
        }
    }
    
    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        
        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(new CloudCliCompleter())
            .build();
    }
    
    private void showWelcomeBanner() {
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║              🌩️  CloudCLI Backup Manager                ║");
        println("║                    Version 1.0.0                         ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
    }
    
    private void showLoggedInMessage() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            printSuccess("✅ Already logged in as: " + user.getUsername());
            println("");
        });
    }
    
    private void showLoginPrompt() {
        println("You are not logged in.");
        println("Commands: login, register, help, exit");
        println("");
    }
    
    private String buildPrompt() {
        if (sessionManager.isLoggedIn()) {
            String username = sessionManager.getCurrentUsername();
            return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("cloudcli")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                .append("@" + username)
                .style(AttributedStyle.DEFAULT)
                .append("> ")
                .toAnsi();
        } else {
            return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                .append("cloudcli")
                .style(AttributedStyle.DEFAULT)
                .append("> ")
                .toAnsi();
        }
    }
    
    private void processCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "help":
                showHelp();
                break;
            case "login":
                handleLogin();
                break;
            case "register":
                handleRegister();
                break;
            case "logout":
                handleLogout();
                break;
            case "whoami":
                handleWhoAmI();
                break;
            case "list":
                handleList();
                break;
            case "menu":
                showMainMenu();
                break;
            case "clear":
                clearScreen();
                break;
            case "exit":
            case "quit":
                running = false;
                break;
            default:
                printError("❌ Unknown command: " + command);
                println("Type 'help' for available commands");
        }
    }
    
    private void showHelp() {
        println("");
        println("┌─────────────────────────────────────────────────────────┐");
        println("│ AVAILABLE COMMANDS                                      │");
        println("├─────────────────────────────────────────────────────────┤");
        println("│  help       - Show this help message                   │");
        println("│  login      - Login to your account                    │");
        println("│  register   - Create a new account                     │");
        println("│  logout     - Logout from current session              │");
        println("│  whoami     - Show current user                        │");
        println("│  list       - List your backups                        │");
        println("│  menu       - Show interactive menu                    │");
        println("│  clear      - Clear screen                             │");
        println("│  exit       - Exit the shell                           │");
        println("└─────────────────────────────────────────────────────────┘");
        println("");
    }
    
    private void handleLogin() {
        try {
            String username = lineReader.readLine("Username: ");
            String password = lineReader.readLine("Password: ", '*');
            
            User user = userService.login(username, password)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            sessionManager.login(user);
            
            println("");
            printSuccess("✅ Logged in successfully!");
            println("   Welcome back, " + user.getUsername() + "!");
            println("");
            
        } catch (Exception e) {
            println("");
            printError("❌ Login failed: " + e.getMessage());
            println("");
        }
    }
    
    private void handleRegister() {
        try {
            String email = lineReader.readLine("Email: ");
            String username = lineReader.readLine("Username: ");
            String password = lineReader.readLine("Password: ", '*');
            String confirmPassword = lineReader.readLine("Confirm Password: ", '*');
            
            if (!password.equals(confirmPassword)) {
                printError("❌ Passwords do not match");
                return;
            }
            
            User user = userService.register(email, username, password);
            
            println("");
            printSuccess("✅ Registration successful!");
            println("   Username: " + user.getUsername());
            println("   Email: " + user.getEmail());
            println("");
            println("You can now login with: login");
            println("");
            
        } catch (Exception e) {
            println("");
            printError("❌ Registration failed: " + e.getMessage());
            println("");
        }
    }
    
    private void handleLogout() {
        if (!sessionManager.isLoggedIn()) {
            printError("❌ You are not logged in");
            return;
        }
        
        String username = sessionManager.getCurrentUsername();
        sessionManager.logout();
        
        println("");
        printSuccess("✅ Logged out successfully!");
        println("   Goodbye, " + username + "!");
        println("");
    }
    
    private void handleWhoAmI() {
        if (!sessionManager.isLoggedIn()) {
            printError("❌ You are not logged in");
            println("   Use 'login' to authenticate");
            return;
        }
        
        sessionManager.getCurrentUser().ifPresent(user -> {
            println("");
            println("👤 Current User:");
            println("   Username: " + user.getUsername());
            println("   Email: " + user.getEmail());
            println("   User ID: " + user.getUserId());
            println("");
        });
    }
    
    private void handleList() {
        if (!sessionManager.isLoggedIn()) {
            printError("❌ You must be logged in to list backups");
            println("   Use 'login' to authenticate");
            return;
        }
        
        String userId = sessionManager.getCurrentUserId();
        List<BackupRecord> backups = backupRepository.findByUserId(userId);
        
        if (backups.isEmpty()) {
            println("");
            println("📦 No backups found");
            println("   Create your first backup with the backup command");
            println("");
            return;
        }
        
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║                    YOUR BACKUPS                          ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
        println("User: " + sessionManager.getCurrentUsername());
        println("Total: " + backups.size() + " backup(s)");
        println("");
        
        for (int i = 0; i < backups.size(); i++) {
            BackupRecord backup = backups.get(i);
            println("─────────────────────────────────────────────────────────");
            println("Backup #" + (i + 1));
            println("  ID: " + backup.getId());
            println("  Database: " + backup.getDatabaseName());
            println("  Type: " + backup.getBackupType());
            println("  Size: " + formatSize(backup.getSizeBytes()));
            println("  Created: " + DATE_FORMATTER.format(backup.getTimestamp()));
            println("  Location: " + backup.getFilePath());
        }
        println("─────────────────────────────────────────────────────────");
        println("");
    }
    
    private void showMainMenu() {
        if (!sessionManager.isLoggedIn()) {
            printError("❌ You must be logged in to access the menu");
            return;
        }
        
        while (true) {
            println("");
            println("┌─────────────────────────────────────────────────────────┐");
            println("│ MAIN MENU                                               │");
            println("├─────────────────────────────────────────────────────────┤");
            println("│  1. 📋 List Backups                                     │");
            println("│  2. 👤 Account Info                                     │");
            println("│  3. 🚪 Logout                                           │");
            println("│  4. ◀️  Back to Shell                                    │");
            println("└─────────────────────────────────────────────────────────┘");
            println("");
            
            try {
                String choice = lineReader.readLine("Choose option (1-4): ");
                
                switch (choice.trim()) {
                    case "1":
                        handleList();
                        break;
                    case "2":
                        handleWhoAmI();
                        break;
                    case "3":
                        handleLogout();
                        return; // Exit menu after logout
                    case "4":
                        return; // Back to shell
                    default:
                        printError("❌ Invalid option. Please choose 1-4");
                }
                
            } catch (UserInterruptException e) {
                return; // Ctrl+C exits menu
            } catch (EndOfFileException e) {
                return; // Ctrl+D exits menu
            }
        }
    }
    
    private void clearScreen() {
        terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
    
    private void showGoodbyeMessage() {
        println("");
        println("👋 Thank you for using CloudCLI!");
        println("   Goodbye!");
        println("");
    }
    
    private void println(String message) {
        terminal.writer().println(message);
        terminal.flush();
    }
    
    private void printSuccess(String message) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        builder.append(message);
        terminal.writer().println(builder.toAnsi());
        terminal.flush();
    }
    
    private void printError(String message) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        builder.append(message);
        terminal.writer().println(builder.toAnsi());
        terminal.flush();
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Auto-completion for commands
     */
    private static class CloudCliCompleter implements Completer {
        private static final String[] COMMANDS = {
            "help", "login", "register", "logout", "whoami", 
            "list", "menu", "clear", "exit", "quit"
        };
        
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String word = line.word();
            
            for (String command : COMMANDS) {
                if (command.startsWith(word)) {
                    candidates.add(new Candidate(command));
                }
            }
        }
    }
}
