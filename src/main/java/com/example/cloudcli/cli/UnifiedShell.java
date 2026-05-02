package com.example.cloudcli.cli;

import com.example.cloudcli.model.User;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Unified Shell - Single entry point for all CloudCLI features
 * Login once, access everything: Shell commands, Interactive wizard, Downloads
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "start",
    description = "Launch unified CloudCLI interface (recommended)",
    mixinStandardHelpOptions = true
)
public class UnifiedShell implements Callable<Integer> {
    
    private final SessionManager sessionManager;
    private final UserService userService;
    private final ApplicationContext applicationContext;
    
    private Terminal terminal;
    private LineReader lineReader;
    private boolean running = true;
    
    @Override
    public Integer call() {
        try {
            initializeTerminal();
            showWelcomeBanner();
            
            // Check if user is already logged in
            if (sessionManager.isLoggedIn()) {
                showLoggedInMessage();
            } else {
                if (!handleAuthMenu()) {
                    return 0; // User chose to exit
                }
            }
            
            // Main unified menu loop
            while (running) {
                try {
                    showUnifiedMenu();
                    String choice = lineReader.readLine(buildPrompt());
                    
                    if (choice == null || choice.trim().isEmpty()) {
                        continue;
                    }
                    
                    processChoice(choice.trim());
                    
                } catch (UserInterruptException e) {
                    println("\n👋 Use 'exit' or option 5 to quit");
                } catch (EndOfFileException e) {
                    running = false;
                }
            }
            
            showGoodbyeMessage();
            return 0;
            
        } catch (Exception e) {
            log.error("Unified shell error", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 1;
        }
    }
    
    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        
        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build();
    }
    
    private void showWelcomeBanner() {
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║              🌩️  CloudCLI Backup Manager                ║");
        println("║                    Version 1.0.0                         ║");
        println("║                                                          ║");
        println("║          Your Complete Backup Solution 🚀               ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
    }
    
    private void showLoggedInMessage() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            printSuccess("✅ Already logged in as: " + user.getUsername());
            println("");
        });
    }
    
    private boolean handleAuthMenu() {
        while (true) {
            println("┌─────────────────────────────────────────────────────────┐");
            println("│ AUTHENTICATION                                          │");
            println("├─────────────────────────────────────────────────────────┤");
            println("│  1. 🔐 Login                                            │");
            println("│  2. 📝 Register New Account                             │");
            println("│  3. 🚪 Exit                                             │");
            println("└─────────────────────────────────────────────────────────┘");
            println("");
            
            try {
                String choice = lineReader.readLine("Choose option (1-3): ");
                
                switch (choice.trim()) {
                    case "1":
                        if (handleLogin()) {
                            return true;
                        }
                        break;
                    case "2":
                        handleRegister();
                        // If registration succeeded, user is now logged in → go to main menu
                        if (sessionManager.isLoggedIn()) {
                            return true;
                        }
                        break;
                    case "3":
                        return false;
                    default:
                        printError("❌ Invalid option. Please choose 1-3");
                }
            } catch (UserInterruptException | EndOfFileException e) {
                return false;
            }
        }
    }
    
    private boolean handleLogin() {
        try {
            println("");
            String username = lineReader.readLine("Username: ");
            String password = lineReader.readLine("Password: ", '*');
            
            User user = userService.login(username, password)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            sessionManager.login(user);
            
            println("");
            printSuccess("✅ Login successful!");
            println("   Welcome back, " + user.getUsername() + "!");
            println("");
            pause();
            return true;
            
        } catch (Exception e) {
            println("");
            printError("❌ Login failed: " + e.getMessage());
            println("");
            pause();
            return false;
        }
    }
    
    private void handleRegister() {
        try {
            println("");
            String email = lineReader.readLine("Email: ");
            String username = lineReader.readLine("Username: ");
            String password = lineReader.readLine("Password: ", '*');
            String confirmPassword = lineReader.readLine("Confirm Password: ", '*');
            
            if (!password.equals(confirmPassword)) {
                printError("❌ Passwords do not match");
                pause();
                return;
            }
            
            User user = userService.register(email, username, password);
            
            println("");
            printSuccess("✅ Registration successful!");
            println("   Welcome, " + user.getUsername() + "! 🎉");
            println("   A welcome email has been sent to: " + user.getEmail());
            println("");
            printSuccess("🔐 Logging you in automatically...");
            println("");
            
            // Auto-login after registration
            sessionManager.login(user);
            printSuccess("✅ You are now logged in as: " + user.getUsername());
            println("");
            pause();
            
        } catch (Exception e) {
            println("");
            printError("❌ Registration failed: " + e.getMessage());
            println("");
            pause();
        }
    }
    
    private void showUnifiedMenu() {
        String username = sessionManager.getCurrentUsername();
        
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║                    MAIN MENU                             ║");
        println("╠══════════════════════════════════════════════════════════╣");
        println("║  User: " + String.format("%-50s", username) + " ║");
        println("╠══════════════════════════════════════════════════════════╣");
        println("║                                                          ║");
        println("║  1. 💻 Command Shell (Quick commands & auto-complete)    ║");
        println("║     • backup, restore, download, list, test-connection   ║");
        println("║     • TAB completion, command history, colored output    ║");
        println("║                                                          ║");
        println("║  2. 🎨 Interactive Wizard (Guided backup creation)       ║");
        println("║     • Step-by-step backup configuration                 ║");
        println("║     • Test connections, schedule backups                ║");
        println("║     • Perfect for beginners                             ║");
        println("║                                                          ║");
        println("║  3. 📋 List My Backups                                   ║");
        println("║  4. 👤 Account Info                                      ║");
        println("║  5. 🚪 Logout & Exit                                     ║");
        println("║                                                          ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
    }
    
    private String buildPrompt() {
        String username = sessionManager.getCurrentUsername();
        return new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
            .append("cloudcli@" + username)
            .style(AttributedStyle.DEFAULT)
            .append(" > ")
            .toAnsi();
    }
    
    private void processChoice(String choice) {
        switch (choice) {
            case "1":
                launchCommandShell();
                break;
            case "2":
                launchInteractiveWizard();
                break;
            case "3":
                showBackupsList();
                break;
            case "4":
                showAccountInfo();
                break;
            case "5":
            case "exit":
            case "quit":
                handleLogout();
                running = false;
                break;
            default:
                printError("❌ Invalid option. Please choose 1-5");
        }
    }
    
    private void launchCommandShell() {
        println("");
        printSuccess("🚀 Launching Command Shell...");
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║              AVAILABLE COMMANDS                          ║");
        println("╠══════════════════════════════════════════════════════════╣");
        println("║                                                          ║");
        println("║  📦 BACKUP OPERATIONS:                                   ║");
        println("║    backup <db-type> <name> [options]                    ║");
        println("║      Types: mysql, postgres, mongodb, sqlite, supabase  ║");
        println("║      Options: --host, --port, --user, --password, etc.  ║");
        println("║                                                          ║");
        println("║  📥 DOWNLOAD & RESTORE:                                  ║");
        println("║    download <backup-id> [--output dir] [--name file]    ║");
        println("║    restore <backup-id> <db-type> <name> [options]       ║");
        println("║                                                          ║");
        println("║  📋 INFORMATION:                                         ║");
        println("║    list              - Show your backups                ║");
        println("║    whoami            - Show current user                ║");
        println("║    test-connection   - Test database connection         ║");
        println("║                                                          ║");
        println("║  🎛️  SHELL CONTROLS:                                     ║");
        println("║    help              - Show detailed help               ║");
        println("║    menu              - Show interactive menu            ║");
        println("║    clear             - Clear screen                     ║");
        println("║    exit              - Return to main menu              ║");
        println("║                                                          ║");
        println("║  💡 TIP: Use TAB for auto-completion, ↑↓ for history    ║");
        println("║                                                          ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
        println("Press Ctrl+D or type 'exit' to return to main menu");
        println("");
        
        try {
            EnhancedInteractiveShell shell = applicationContext.getBean(EnhancedInteractiveShell.class);
            shell.call();
        } catch (Exception e) {
            printError("❌ Error launching shell: " + e.getMessage());
        }
        
        println("");
        printSuccess("↩️  Returned to main menu");
        pause();
    }
    
    private void launchInteractiveWizard() {
        println("");
        printSuccess("🎨 Launching Interactive Wizard...");
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║           INTERACTIVE WIZARD FEATURES                    ║");
        println("╠══════════════════════════════════════════════════════════╣");
        println("║                                                          ║");
        println("║  1. 🔄 Create New Backup                                 ║");
        println("║     • Step-by-step guided process                       ║");
        println("║     • Choose database type and configure settings       ║");
        println("║     • Automatic backup execution                        ║");
        println("║                                                          ║");
        println("║  2. 📜 View Backup History                               ║");
        println("║     • See all your backups with details                 ║");
        println("║     • Filter by database or date                        ║");
        println("║                                                          ║");
        println("║  3. 🔌 Test Database Connection                          ║");
        println("║     • Verify connection before backup                   ║");
        println("║     • Supports all database types                       ║");
        println("║                                                          ║");
        println("║  4. 📥 Download Backup                                   ║");
        println("║     • Download backups from cloud storage               ║");
        println("║     • Choose custom location                            ║");
        println("║                                                          ║");
        println("║  5. 👤 Account Info                                      ║");
        println("║  6. 🔄 Restore Backup (Coming Soon)                      ║");
        println("║  7. 📅 Schedule Backup (Coming Soon)                     ║");
        println("║  8. 🚪 Return to Main Menu                               ║");
        println("║                                                          ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
        pause();
        
        try {
            InteractiveCommand wizard = applicationContext.getBean(InteractiveCommand.class);
            wizard.call();
        } catch (Exception e) {
            printError("❌ Error launching wizard: " + e.getMessage());
        }
        
        println("");
        printSuccess("↩️  Returned to main menu");
        pause();
    }
    
    private void showBackupsList() {
        println("");
        try {
            ListBackupsCommand listCmd = applicationContext.getBean(ListBackupsCommand.class);
            listCmd.call();
        } catch (Exception e) {
            printError("❌ Error listing backups: " + e.getMessage());
        }
        pause();
    }
    
    private void showAccountInfo() {
        println("");
        sessionManager.getCurrentUser().ifPresent(user -> {
            println("╔══════════════════════════════════════════════════════════╗");
            println("║                   ACCOUNT INFORMATION                    ║");
            println("╚══════════════════════════════════════════════════════════╝");
            println("");
            println("  Username: " + user.getUsername());
            println("  Email: " + user.getEmail());
            println("  User ID: " + user.getUserId());
            println("  Created: " + user.getCreatedAt());
            println("");
        });
        pause();
    }
    
    private void handleLogout() {
        if (sessionManager.isLoggedIn()) {
            String username = sessionManager.getCurrentUsername();
            sessionManager.logout();
            println("");
            printSuccess("✅ Logged out successfully!");
            println("   Goodbye, " + username + "!");
        }
    }
    
    private void showGoodbyeMessage() {
        println("");
        println("╔══════════════════════════════════════════════════════════╗");
        println("║                                                          ║");
        println("║              Thank you for using CloudCLI! 🙏           ║");
        println("║                                                          ║");
        println("║              Your data is safe with us! 🔒              ║");
        println("║                                                          ║");
        println("╚══════════════════════════════════════════════════════════╝");
        println("");
    }
    
    private void pause() {
        try {
            lineReader.readLine("\nPress Enter to continue...");
        } catch (UserInterruptException | EndOfFileException e) {
            // Ignore
        }
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
}
