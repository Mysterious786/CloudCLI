package com.example.cloudcli.cli;

import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.model.User;
import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.BackupOrchestrator;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.UserService;
import com.example.cloudcli.service.connection.ConnectionTester;
import com.example.cloudcli.service.connection.ConnectionTesterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.Console;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Interactive mode with colorful, responsive menu interface
 * Now includes login/authentication support
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "interactive",
    aliases = {"i", "menu"},
    description = "Interactive menu-driven interface",
    mixinStandardHelpOptions = true
)
public class InteractiveCommand implements Callable<Integer> {
    
    private final BackupOrchestrator backupOrchestrator;
    private final ConnectionTesterFactory connectionTesterFactory;
    private final SessionManager sessionManager;
    private final UserService userService;
    private final InMemoryBackupRepository backupRepository;
    private final ApplicationContext applicationContext;
    private final Scanner scanner = new Scanner(System.in);
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());
    
    @Override
    public Integer call() {
        AnsiConsole.systemInstall(); // Enable colors
        
        try {
            showWelcome();
            
            // Check if user is logged in
            if (!sessionManager.isLoggedIn()) {
                if (!handleAuthMenu()) {
                    return 0; // User chose to exit
                }
            } else {
                info("Already logged in as: " + sessionManager.getCurrentUsername());
            }
            
            while (true) {
                showMainMenu();
                String choice = prompt("Enter choice (1-8)");
                
                switch (choice.trim()) {
                    case "1" -> performBackup();
                    case "2" -> testConnection();
                    case "3" -> viewBackupHistory();
                    case "4" -> downloadBackup();
                    case "5" -> showAccountInfo();
                    case "6" -> showSettings();
                    case "7" -> showHelp();
                    case "8", "q", "quit", "exit" -> {
                        handleLogout();
                        showGoodbye();
                        return 0;
                    }
                    default -> error("Invalid choice. Please enter 1-8.");
                }
            }
        } finally {
            AnsiConsole.systemUninstall(); // Cleanup
        }
    }
    
    private boolean handleAuthMenu() {
        while (true) {
            System.out.println(ansi().fg(BLUE).bold().a("\n=============== AUTHENTICATION ===============").reset());
            System.out.println();
            System.out.println(ansi().fg(GREEN).a("  1. ").fg(WHITE).a("🔐 Login").reset());
            System.out.println(ansi().fg(GREEN).a("  2. ").fg(WHITE).a("📝 Register").reset());
            System.out.println(ansi().fg(RED).a("  3. ").fg(WHITE).a("🚪 Exit").reset());
            System.out.println();
            System.out.println(ansi().fg(BLUE).bold().a("==============================================").reset());
            
            String choice = prompt("Enter choice (1-3)");
            
            switch (choice.trim()) {
                case "1" -> {
                    if (handleLogin()) {
                        return true;
                    }
                }
                case "2" -> {
                    handleRegister();
                    // Auto-login succeeded inside handleRegister → go straight to main menu
                    if (sessionManager.isLoggedIn()) {
                        return true;
                    }
                }
                case "3" -> {
                    return false;
                }
                default -> error("Invalid choice. Please enter 1-3.");
            }
        }
    }
    
    private boolean handleLogin() {
        header("🔐 LOGIN");
        
        String username = prompt("Username");
        String password = promptPassword("Password");
        
        try {
            User user = userService.login(username, password)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            sessionManager.login(user);
            
            success("Login successful!");
            info("Welcome back, " + user.getUsername() + "!");
            pause();
            return true;
            
        } catch (Exception e) {
            error("Login failed: " + e.getMessage());
            pause();
            return false;
        }
    }
    
    private void handleRegister() {
        header("📝 REGISTER");
        
        String email = prompt("Email");
        String username = prompt("Username");
        String password = promptPassword("Password");
        String confirmPassword = promptPassword("Confirm Password");
        
        if (!password.equals(confirmPassword)) {
            error("Passwords do not match!");
            pause();
            return;
        }
        
        try {
            User user = userService.register(email, username, password);
            
            success("Registration successful! Welcome, " + user.getUsername() + "! 🎉");
            System.out.println("  Email: " + ansi().fg(CYAN).a(user.getEmail()).reset());
            info("A welcome email has been sent to: " + user.getEmail());
            System.out.println();
            System.out.println(ansi().fg(GREEN).bold().a("🔐 Logging you in automatically...").reset());
            
            // Auto-login after registration
            sessionManager.login(user);
            
            success("You are now logged in as: " + user.getUsername());
            pause();
            
        } catch (Exception e) {
            error("Registration failed: " + e.getMessage());
            pause();
        }
    }
    
    private void handleLogout() {
        if (sessionManager.isLoggedIn()) {
            String username = sessionManager.getCurrentUsername();
            sessionManager.logout();
            info("Logged out successfully. Goodbye, " + username + "!");
        }
    }
    
    private void showAccountInfo() {
        header("👤 ACCOUNT INFO");
        
        if (!sessionManager.isLoggedIn()) {
            error("You must be logged in!");
            pause();
            return;
        }
        
        sessionManager.getCurrentUser().ifPresent(user -> {
            System.out.println(ansi().fg(CYAN).a("\nUser Information:").reset());
            System.out.println("  Username: " + ansi().fg(GREEN).a(user.getUsername()).reset());
            System.out.println("  Email: " + ansi().fg(GREEN).a(user.getEmail()).reset());
            System.out.println("  User ID: " + ansi().fg(GREEN).a(user.getUserId()).reset());
            System.out.println("  Created: " + ansi().fg(GREEN).a(DATE_FORMATTER.format(user.getCreatedAt())).reset());
        });
        
        pause();
    }
    
    private void downloadBackup() {
        header("⬇️  DOWNLOAD BACKUP");

        if (!sessionManager.isLoggedIn()) {
            error("You must be logged in!");
            pause();
            return;
        }

        String userId = sessionManager.getCurrentUserId();
        List<BackupRecord> backups = backupRepository.findByUserId(userId);

        if (backups.isEmpty()) {
            info("No backups found. Create a backup first!");
            pause();
            return;
        }

        // Show numbered list with friendly names
        System.out.println(ansi().fg(CYAN).a("\nYour Backups:").reset());
        System.out.println(ansi().fg(YELLOW).a("─────────────────────────────────────────────────────────").reset());
        System.out.printf(ansi().fg(CYAN).a("  %-4s %-20s %-12s %-10s %-20s%n").reset().toString(),
            "#", "DATABASE", "TYPE", "SIZE", "DATE");
        System.out.println(ansi().fg(YELLOW).a("─────────────────────────────────────────────────────────").reset());

        for (int i = 0; i < backups.size(); i++) {
            BackupRecord b = backups.get(i);
            String date = DATE_FORMATTER.format(b.getTimestamp()).substring(0, 16);
            System.out.printf("  %-4s %-20s %-12s %-10s %-20s%n",
                ansi().fg(GREEN).a(String.valueOf(i + 1)).reset(),
                truncate(b.getDatabaseName(), 20),
                b.getBackupType(),
                formatSize(b.getSizeBytes()),
                date);
        }

        System.out.println(ansi().fg(YELLOW).a("─────────────────────────────────────────────────────────").reset());
        System.out.println();

        // Let user pick by number
        String choice = prompt("Enter number to download (or 0 to cancel)");
        int idx;
        try {
            idx = Integer.parseInt(choice.trim()) - 1;
        } catch (NumberFormatException e) {
            error("Invalid choice.");
            pause();
            return;
        }

        if (idx < 0) {
            info("Download cancelled.");
            pause();
            return;
        }

        if (idx >= backups.size()) {
            error("Invalid number. Please choose 1-" + backups.size());
            pause();
            return;
        }

        BackupRecord selected = backups.get(idx);

        // Ask for custom name
        String defaultName = selected.getDatabaseName() + "_" + selected.getBackupType().toLowerCase() + "_backup";
        String customName = promptDefault("Save as (filename)", defaultName);
        String outputDir = promptDefault("Save to directory", "./downloads");

        System.out.println(ansi().fg(CYAN).a("\n⏳ Downloading...").reset());
        System.out.println("  File: " + ansi().fg(YELLOW).a(selected.getFilePath()).reset());

        try {
            // Ensure output directory exists
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outputDir));

            // Get file extension from original path
            String originalPath = selected.getFilePath();
            String ext = originalPath.contains(".") ?
                originalPath.substring(originalPath.lastIndexOf('.')) : "";

            String finalFileName = customName.endsWith(ext) ? customName : customName + ext;
            java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir, finalFileName);

            // Download from storage
            com.example.cloudcli.service.storage.StorageProvider storageProvider =
                applicationContext.getBean("storageProvider", com.example.cloudcli.service.storage.StorageProvider.class);

            try (java.io.InputStream in = storageProvider.retrieve(selected.getFilePath());
                 java.io.FileOutputStream out = new java.io.FileOutputStream(outputPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long total = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }

                System.out.println();
                success("Download completed!");
                System.out.println("  Name    : " + ansi().fg(CYAN).a(finalFileName).reset());
                System.out.println("  Size    : " + ansi().fg(CYAN).a(formatSize(total)).reset());
                System.out.println("  Saved to: " + ansi().fg(CYAN).a(outputPath.toAbsolutePath().toString()).reset());
            }

        } catch (Exception e) {
            error("Download failed: " + e.getMessage());
        }

        pause();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
    
    private void showWelcome() {
        System.out.println(ansi().fg(CYAN).bold().a("\n+============================================================+").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|              ").fg(YELLOW).a("☁️  CloudCLI Backup Tool  ☁️").fg(CYAN).a("              |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|          ").fg(WHITE).a("Multi-Database Backup Solution").fg(CYAN).a("                |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("+============================================================+").reset());
        System.out.println();
    }
    
    private void showMainMenu() {
        String username = sessionManager.getCurrentUsername();
        
        System.out.println(ansi().fg(BLUE).bold().a("\n=============== MAIN MENU ===============").reset());
        System.out.println(ansi().fg(CYAN).a("  Logged in as: ").fg(GREEN).bold().a(username).reset());
        System.out.println();
        System.out.println(ansi().fg(GREEN).a("  1. ").fg(WHITE).a("🗄️  Backup Database").reset());
        System.out.println(ansi().fg(GREEN).a("  2. ").fg(WHITE).a("🔌 Test Connection").reset());
        System.out.println(ansi().fg(GREEN).a("  3. ").fg(WHITE).a("📋 View Backup History").reset());
        System.out.println(ansi().fg(GREEN).a("  4. ").fg(WHITE).a("⬇️  Download Backup").reset());
        System.out.println(ansi().fg(GREEN).a("  5. ").fg(WHITE).a("👤 Account Info").reset());
        System.out.println(ansi().fg(GREEN).a("  6. ").fg(WHITE).a("⚙️  Settings").reset());
        System.out.println(ansi().fg(GREEN).a("  7. ").fg(WHITE).a("❓ Help").reset());
        System.out.println(ansi().fg(RED).a("  8. ").fg(WHITE).a("🚪 Logout & Exit").reset());
        System.out.println();
        System.out.println(ansi().fg(BLUE).bold().a("=========================================").reset());
    }
    
    private void performBackup() {
        header("🗄️  DATABASE BACKUP");
        
        DatabaseConfig config = new DatabaseConfig();
        
        // Select database type
        System.out.println(ansi().fg(CYAN).a("\nDatabase Type:").reset());
        System.out.println("  1. MySQL");
        System.out.println("  2. PostgreSQL");
        System.out.println("  3. MongoDB");
        System.out.println("  4. SQLite");
        System.out.println("  5. Supabase");
        System.out.println("  0. Cancel");
        
        String dbChoice = prompt("Select database (1-5)");
        
        switch (dbChoice.trim()) {
            case "1" -> config.setType("mysql");
            case "2" -> config.setType("postgres");
            case "3" -> config.setType("mongodb");
            case "4" -> config.setType("sqlite");
            case "5" -> config.setType("supabase");
            case "0" -> { return; }
            default -> {
                error("Invalid choice");
                return;
            }
        }
        
        // Get database details
        if (config.getType().equals("sqlite")) {
            config.setDatabase(prompt("Database file path"));
        } else {
            config.setHost(promptDefault("Host", "localhost"));
            config.setPort(Integer.parseInt(promptDefault("Port", String.valueOf(getDefaultPort(config.getType())))));
            config.setDatabase(prompt("Database name"));
            config.setUsername(prompt("Username"));
            config.setPassword(promptPassword("Password"));
        }
        
        // Backup type
        System.out.println(ansi().fg(CYAN).a("\nBackup Type:").reset());
        System.out.println("  1. FULL");
        System.out.println("  2. SCHEMA_ONLY");
        System.out.println("  3. DATA_ONLY");
        
        String backupChoice = promptDefault("Select type (1-3)", "1");
        BackupType backupType = switch (backupChoice.trim()) {
            case "2" -> BackupType.SCHEMA_ONLY;
            case "3" -> BackupType.DATA_ONLY;
            default -> BackupType.FULL;
        };
        
        boolean compress = promptDefault("Compress? (y/n)", "y").toLowerCase().startsWith("y");
        
        // Summary
        System.out.println(ansi().fg(YELLOW).a("\n═══════════════════════════════════════").reset());
        System.out.println(ansi().bold().a("Backup Summary:").reset());
        System.out.println("  Database: " + ansi().fg(CYAN).a(config.getType() + " - " + config.getDatabase()).reset());
        if (!config.getType().equals("sqlite")) {
            System.out.println("  Host: " + ansi().fg(CYAN).a(config.getHost() + ":" + config.getPort()).reset());
        }
        System.out.println("  Type: " + ansi().fg(CYAN).a(backupType.toString()).reset());
        System.out.println("  Compress: " + ansi().fg(CYAN).a(compress ? "Yes" : "No").reset());
        System.out.println(ansi().fg(YELLOW).a("═══════════════════════════════════════").reset());
        
        if (!promptDefault("Proceed? (y/n)", "y").toLowerCase().startsWith("y")) {
            info("Backup cancelled");
            return;
        }
        
        // Perform backup
        System.out.println(ansi().fg(CYAN).a("\n⏳ Starting backup...").reset());
        
        try {
            BackupResult result = backupOrchestrator.performBackup(config, backupType, compress);
            
            success("BACKUP SUCCESSFUL!");
            System.out.println(ansi().fg(GREEN).a("═══════════════════════════════════════").reset());
            System.out.println("  Database: " + result.getDatabaseName());
            System.out.println("  Type: " + result.getBackupType());
            System.out.println("  Size: " + formatSize(result.getSizeInBytes()));
            System.out.println("  Duration: " + result.getDurationSeconds() + "s");
            System.out.println("  Location: " + result.getStorageLocation());
            System.out.println(ansi().fg(GREEN).a("═══════════════════════════════════════").reset());
            
        } catch (Exception e) {
            error("Backup failed: " + e.getMessage());
        }
        
        pause();
    }
    
    private void testConnection() {
        header("🔌 TEST CONNECTION");
        
        DatabaseConfig config = new DatabaseConfig();
        
        System.out.println(ansi().fg(CYAN).a("\nDatabase Type:").reset());
        System.out.println("  1. MySQL");
        System.out.println("  2. PostgreSQL");
        System.out.println("  3. MongoDB");
        System.out.println("  4. SQLite");
        System.out.println("  5. Supabase");
        System.out.println("  0. Cancel");
        
        String choice = prompt("Select database (1-5)");
        
        switch (choice.trim()) {
            case "1" -> config.setType("mysql");
            case "2" -> config.setType("postgres");
            case "3" -> config.setType("mongodb");
            case "4" -> config.setType("sqlite");
            case "5" -> config.setType("supabase");
            case "0" -> { return; }
            default -> {
                error("Invalid choice");
                return;
            }
        }
        
        if (config.getType().equals("sqlite")) {
            config.setDatabase(prompt("Database file path"));
        } else {
            config.setHost(promptDefault("Host", "localhost"));
            config.setPort(Integer.parseInt(promptDefault("Port", String.valueOf(getDefaultPort(config.getType())))));
            config.setDatabase(prompt("Database name"));
            config.setUsername(prompt("Username"));
            config.setPassword(promptPassword("Password"));
        }
        
        System.out.println(ansi().fg(CYAN).a("\n⏳ Testing connection...").reset());
        
        try {
            ConnectionTester tester = connectionTesterFactory.getTester(config.getType());
            boolean result = tester.test(config);
            
            if (result) {
                success("Connection successful!");
                System.out.println(ansi().fg(GREEN).a("Database is accessible and ready for backup.").reset());
            } else {
                error("Connection failed!");
            }
            
        } catch (Exception e) {
            error("Connection test failed: " + e.getMessage());
        }
        
        pause();
    }
    
    private void viewBackupHistory() {
        header("📋 BACKUP HISTORY");
        
        if (!sessionManager.isLoggedIn()) {
            error("You must be logged in!");
            pause();
            return;
        }
        
        String userId = sessionManager.getCurrentUserId();
        List<BackupRecord> backups = backupRepository.findByUserId(userId);
        
        if (backups.isEmpty()) {
            info("No backups found. Create your first backup!");
            pause();
            return;
        }
        
        System.out.println(ansi().fg(CYAN).a("\nTotal backups: ").fg(GREEN).bold().a(String.valueOf(backups.size())).reset());
        System.out.println();
        
        for (int i = 0; i < backups.size(); i++) {
            BackupRecord backup = backups.get(i);
            System.out.println(ansi().fg(YELLOW).a("─────────────────────────────────────────").reset());
            System.out.println(ansi().fg(GREEN).bold().a("Backup #" + (i + 1)).reset());
            System.out.println("  Database: " + ansi().fg(CYAN).a(backup.getDatabaseName()).reset());
            System.out.println("  Type: " + ansi().fg(CYAN).a(backup.getBackupType()).reset());
            System.out.println("  Size: " + ansi().fg(CYAN).a(formatSize(backup.getSizeBytes())).reset());
            System.out.println("  Created: " + ansi().fg(CYAN).a(DATE_FORMATTER.format(backup.getTimestamp())).reset());
            System.out.println("  Duration: " + ansi().fg(CYAN).a(backup.getDurationSeconds() + "s").reset());
            System.out.println("  ID: " + ansi().fg(MAGENTA).a(backup.getId()).reset());
        }
        System.out.println(ansi().fg(YELLOW).a("─────────────────────────────────────────").reset());
        
        pause();
    }
    
    private void showSettings() {
        header("⚙️  SETTINGS");
        System.out.println(ansi().fg(CYAN).a("\nCurrent Configuration:").reset());
        System.out.println("  Storage: " + ansi().fg(GREEN).a("Local (./backups)").reset());
        System.out.println("  Notifications: " + ansi().fg(GREEN).a("Console").reset());
        System.out.println("  Compression: " + ansi().fg(GREEN).a("Enabled").reset());
        System.out.println(ansi().fg(YELLOW).a("\nEdit application.yml to change settings.").reset());
        pause();
    }
    
    private void showHelp() {
        header("❓ HELP");
        System.out.println(ansi().fg(CYAN).bold().a("\nQuick Start:").reset());
        System.out.println(ansi().fg(GREEN).a("  1. ").reset() + "Select 'Backup Database' from main menu");
        System.out.println(ansi().fg(GREEN).a("  2. ").reset() + "Choose your database type");
        System.out.println(ansi().fg(GREEN).a("  3. ").reset() + "Enter connection details");
        System.out.println(ansi().fg(GREEN).a("  4. ").reset() + "Confirm and backup!");
        System.out.println(ansi().fg(CYAN).bold().a("\nSupported Databases:").reset());
        System.out.println("  • MySQL, PostgreSQL, MongoDB");
        System.out.println("  • SQLite, Supabase");
        System.out.println(ansi().fg(CYAN).bold().a("\nDocumentation:").reset());
        System.out.println("  • README.md");
        System.out.println("  • SETUP_GUIDE.md");
        pause();
    }
    
    private void showGoodbye() {
        System.out.println(ansi().fg(CYAN).bold().a("\n+============================================================+").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                  ").fg(GREEN).a("Thank you for using").fg(CYAN).a("                    |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                  ").fg(YELLOW).a("CloudCLI Backup Tool").fg(CYAN).a("                  |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|              ").fg(WHITE).a("Your data is safe with us! 🔒").fg(CYAN).a("             |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("|                                                            |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("+============================================================+").reset());
        System.out.println();
    }
    
    // Helper methods
    
    private void header(String title) {
        System.out.println(ansi().fg(CYAN).bold().a("\n+============================================================+").reset());
        String padding = " ".repeat(Math.max(0, 56 - title.length()));
        System.out.println(ansi().fg(CYAN).bold().a("|  " + title + padding + "  |").reset());
        System.out.println(ansi().fg(CYAN).bold().a("+============================================================+").reset());
    }
    
    private void success(String msg) {
        System.out.println(ansi().fg(GREEN).bold().a("\n✅ " + msg).reset());
    }
    
    private void error(String msg) {
        System.out.println(ansi().fg(RED).bold().a("\n❌ " + msg).reset());
    }
    
    private void info(String msg) {
        System.out.println(ansi().fg(BLUE).a("\nℹ️  " + msg).reset());
    }
    
    private String prompt(String message) {
        System.out.print(ansi().fg(YELLOW).a(message + ": ").reset());
        return scanner.nextLine().trim();
    }
    
    private String promptDefault(String message, String defaultValue) {
        System.out.print(ansi().fg(YELLOW).a(message + " [" + defaultValue + "]: ").reset());
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    private String promptPassword(String message) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(ansi().fg(YELLOW).a(message + ": ").reset().toString());
            return new String(pwd);
        }
        return prompt(message);
    }
    
    private void pause() {
        System.out.print(ansi().fg(CYAN).a("\nPress Enter to continue...").reset());
        scanner.nextLine();
    }
    
    private int getDefaultPort(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql" -> 3306;
            case "postgres", "supabase" -> 5432;
            case "mongodb" -> 27017;
            default -> 0;
        };
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
