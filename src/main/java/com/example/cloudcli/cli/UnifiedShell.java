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
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Unified Shell - Clean, modern design that works in ALL terminals
 * No box-drawing chars, no emoji in borders, pure ANSI colors
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "start",
    description = "Launch CloudCLI interface",
    mixinStandardHelpOptions = true
)
public class UnifiedShell implements Callable<Integer> {

    private final SessionManager sessionManager;
    private final UserService userService;
    private final ApplicationContext applicationContext;

    private Terminal terminal;
    private LineReader lineReader;
    private boolean running = true;

    // ANSI color codes - work in ALL terminals
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String PURPLE  = "\u001B[35m";
    private static final String CYAN    = "\u001B[36m";
    private static final String GREEN   = "\u001B[32m";
    private static final String RED     = "\u001B[31m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String BLUE    = "\u001B[34m";
    private static final String WHITE   = "\u001B[37m";
    private static final String BG_DARK = "\u001B[40m";

    // Fixed width - no emojis in borders
    private static final int WIDTH = 60;
    private static final String LINE  = "=".repeat(WIDTH);
    private static final String DLINE = "-".repeat(WIDTH);

    @Override
    public Integer call() {
        try {
            initializeTerminal();
            clearScreen();
            showWelcomeBanner();

            if (sessionManager.isLoggedIn()) {
                showLoggedInMessage();
            } else {
                if (!handleAuthMenu()) {
                    showGoodbye();
                    return 0;
                }
            }

            while (running) {
                try {
                    showMainMenu();
                    String choice = lineReader.readLine(buildPrompt());
                    if (choice == null) { running = false; break; }
                    processChoice(choice.trim());
                } catch (UserInterruptException e) {
                    print(YELLOW + "  Use option 5 to exit." + RESET);
                } catch (EndOfFileException e) {
                    running = false;
                }
            }

            showGoodbye();
            return 0;

        } catch (Exception e) {
            log.error("Shell error", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder().system(true).dumb(true).build();
        lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    private void clearScreen() {
        print("\u001B[2J\u001B[H");
    }

    // ── WELCOME BANNER ────────────────────────────────────────────────────────
    private void showWelcomeBanner() {
        println("");
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println(PURPLE + BOLD + "  CloudCLI  --  Multi-Database Backup Manager" + RESET);
        println(PURPLE + BOLD + "  Version 1.0.3  |  by Saqlain Zarjis Ansari" + RESET);
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println("");
    }

    private void showLoggedInMessage() {
        sessionManager.getCurrentUser().ifPresent(user ->
            println(GREEN + BOLD + "  Logged in as: " + user.getUsername() + RESET + "\n")
        );
    }

    // ── AUTH MENU ─────────────────────────────────────────────────────────────
    private boolean handleAuthMenu() {
        while (true) {
            println(CYAN + BOLD + "  AUTHENTICATION" + RESET);
            println(DIM + "  " + DLINE + RESET);
            println("");
            println(GREEN + "  [1]" + RESET + "  Login to your account");
            println(GREEN + "  [2]" + RESET + "  Create a new account");
            println(RED   + "  [3]" + RESET + "  Exit");
            println("");
            println(DIM + "  " + DLINE + RESET);
            println("");

            try {
                String choice = lineReader.readLine("  Choose (1-3): ");
                if (choice == null) return false;

                switch (choice.trim()) {
                    case "1" -> { if (handleLogin()) return true; }
                    case "2" -> { handleRegister(); if (sessionManager.isLoggedIn()) return true; }
                    case "3" -> { return false; }
                    default  -> println(RED + "  Invalid option. Enter 1, 2 or 3." + RESET + "\n");
                }
            } catch (UserInterruptException | EndOfFileException e) {
                return false;
            }
        }
    }

    private boolean handleLogin() {
        println("");
        println(CYAN + BOLD + "  LOGIN" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");

        try {
            String username = lineReader.readLine("  Username : ");
            String password = lineReader.readLine("  Password : ", '*');

            User user = userService.login(username, password)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            sessionManager.login(user);

            println("");
            println(GREEN + BOLD + "  Login successful! Welcome back, " + user.getUsername() + RESET);
            println("");
            pause();
            return true;

        } catch (Exception e) {
            println("");
            println(RED + BOLD + "  Login failed: " + e.getMessage() + RESET);
            println("");
            pause();
            return false;
        }
    }

    private void handleRegister() {
        println("");
        println(CYAN + BOLD + "  REGISTER" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");

        try {
            String email    = lineReader.readLine("  Email    : ");
            String username = lineReader.readLine("  Username : ");
            String password = lineReader.readLine("  Password : ", '*');
            String confirm  = lineReader.readLine("  Confirm  : ", '*');

            if (!password.equals(confirm)) {
                println(RED + "\n  Passwords do not match." + RESET + "\n");
                pause();
                return;
            }

            User user = userService.register(email, username, password);
            sessionManager.login(user);

            println("");
            println(GREEN + BOLD + "  Registration successful! Welcome, " + user.getUsername() + "!" + RESET);
            println(DIM   + "  A welcome email has been sent to: " + user.getEmail() + RESET);
            println(GREEN + "  You are now logged in." + RESET);
            println("");
            pause();

        } catch (Exception e) {
            println("");
            println(RED + BOLD + "  Registration failed: " + e.getMessage() + RESET);
            println("");
            pause();
        }
    }

    // ── MAIN MENU ─────────────────────────────────────────────────────────────
    private void showMainMenu() {
        String username = sessionManager.getCurrentUsername();

        clearScreen();
        println("");
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println(PURPLE + BOLD + "  CloudCLI  --  Backup Manager" + RESET);
        println(CYAN   + "  Logged in as: " + BOLD + username + RESET);
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println("");

        println(CYAN + BOLD + "  MAIN MENU" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");
        println(GREEN  + "  [1]" + RESET + "  Command Shell          " + DIM + "backup, list, download, restore" + RESET);
        println(GREEN  + "  [2]" + RESET + "  Interactive Wizard      " + DIM + "step-by-step guided backup" + RESET);
        println(GREEN  + "  [3]" + RESET + "  My Backups              " + DIM + "view all your backups" + RESET);
        println(GREEN  + "  [4]" + RESET + "  Account Info            " + DIM + "view your account details" + RESET);
        println(YELLOW + "  [5]" + RESET + "  Logout & Exit");
        println("");
        println(DIM + "  " + DLINE + RESET);
        println("");
    }

    private String buildPrompt() {
        String username = sessionManager.getCurrentUsername();
        return new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
            .append("  cloudcli@" + username)
            .style(AttributedStyle.DEFAULT)
            .append(" > ")
            .toAnsi();
    }

    private void processChoice(String choice) {
        switch (choice) {
            case "1" -> launchCommandShell();
            case "2" -> launchInteractiveWizard();
            case "3" -> showBackupsList();
            case "4" -> showAccountInfo();
            case "5", "exit", "quit", "logout" -> {
                handleLogout();
                running = false;
            }
            default -> {
                println(RED + "\n  Invalid option. Choose 1-5." + RESET);
                pause();
            }
        }
    }

    // ── COMMAND SHELL ─────────────────────────────────────────────────────────
    private void launchCommandShell() {
        clearScreen();
        println("");
        println(CYAN + BOLD + "  COMMAND SHELL" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");
        println(BOLD + "  Available Commands:" + RESET);
        println("");
        println(GREEN + "  backup" + RESET + "   --type=sqlite --database=mydb.db");
        println(GREEN + "  backup" + RESET + "   --type=postgres --database=mydb --host=localhost");
        println(GREEN + "  list" + RESET + "     Show all your backups");
        println(GREEN + "  download" + RESET + " --id <backup-id>  Download a backup");
        println(GREEN + "  whoami" + RESET + "   Show current user");
        println(GREEN + "  help" + RESET + "     Show all commands");
        println(GREEN + "  exit" + RESET + "     Return to main menu");
        println("");
        println(DIM + "  TIP: Use TAB for auto-completion, UP/DOWN for history" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");

        try {
            EnhancedInteractiveShell shell = applicationContext.getBean(EnhancedInteractiveShell.class);
            shell.call();
        } catch (Exception e) {
            println(RED + "\n  Shell error: " + e.getMessage() + RESET);
        }

        println("");
        println(GREEN + "  Returned to main menu." + RESET);
        pause();
    }

    // ── INTERACTIVE WIZARD ────────────────────────────────────────────────────
    private void launchInteractiveWizard() {
        clearScreen();
        println("");
        println(CYAN + BOLD + "  INTERACTIVE WIZARD" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");
        println(DIM + "  Starting guided backup wizard..." + RESET);
        println("");
        pause();

        try {
            InteractiveCommand wizard = applicationContext.getBean(InteractiveCommand.class);
            wizard.call();
        } catch (Exception e) {
            println(RED + "\n  Wizard error: " + e.getMessage() + RESET);
        }

        println("");
        println(GREEN + "  Returned to main menu." + RESET);
        pause();
    }

    // ── BACKUPS LIST ──────────────────────────────────────────────────────────
    private void showBackupsList() {
        clearScreen();
        println("");
        println(CYAN + BOLD + "  MY BACKUPS" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");

        try {
            ListBackupsCommand listCmd = applicationContext.getBean(ListBackupsCommand.class);
            listCmd.call();
        } catch (Exception e) {
            println(RED + "  Error: " + e.getMessage() + RESET);
        }

        pause();
    }

    // ── ACCOUNT INFO ──────────────────────────────────────────────────────────
    private void showAccountInfo() {
        clearScreen();
        println("");
        println(CYAN + BOLD + "  ACCOUNT INFORMATION" + RESET);
        println(DIM + "  " + DLINE + RESET);
        println("");

        sessionManager.getCurrentUser().ifPresent(user -> {
            println(WHITE + "  Username  : " + BOLD + user.getUsername() + RESET);
            println(WHITE + "  Email     : " + user.getEmail() + RESET);
            println(WHITE + "  User ID   : " + DIM + user.getUserId() + RESET);
            println(WHITE + "  Created   : " + user.getCreatedAt() + RESET);
        });

        println("");
        println(DIM + "  " + DLINE + RESET);
        println("");
        pause();
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────
    private void handleLogout() {
        if (sessionManager.isLoggedIn()) {
            String username = sessionManager.getCurrentUsername();
            sessionManager.logout();
            println("");
            println(GREEN + BOLD + "  Logged out. Goodbye, " + username + "!" + RESET);
            println("");
        }
    }

    // ── GOODBYE ───────────────────────────────────────────────────────────────
    private void showGoodbye() {
        println("");
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println(PURPLE + BOLD + "  Thank you for using CloudCLI!" + RESET);
        println(PURPLE + BOLD + "  Your data is safe with us." + RESET);
        println(PURPLE + BOLD + "  " + LINE + RESET);
        println("");
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void pause() {
        try {
            lineReader.readLine("  Press Enter to continue...");
        } catch (UserInterruptException | EndOfFileException e) {
            // ignore
        }
    }

    private void println(String message) {
        terminal.writer().println(message);
        terminal.flush();
    }

    private void print(String message) {
        terminal.writer().print(message);
        terminal.flush();
    }
}
