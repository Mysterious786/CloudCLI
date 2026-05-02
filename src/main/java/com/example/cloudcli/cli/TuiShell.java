package com.example.cloudcli.cli;

import com.example.cloudcli.model.User;
import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.UserService;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Full-screen TUI Shell using Lanterna
 * Provides a proper terminal UI like htop/lazygit
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "tui",
    aliases = {"ui"},
    description = "Launch full-screen terminal UI",
    mixinStandardHelpOptions = true
)
public class TuiShell implements Callable<Integer> {

    private final SessionManager sessionManager;
    private final UserService userService;
    private final InMemoryBackupRepository backupRepository;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault());

    // ── Purple/dark theme ─────────────────────────────────────────────────────
    private static final TextColor BG         = new TextColor.RGB(15, 12, 41);    // dark navy
    private static final TextColor PANEL_BG   = new TextColor.RGB(30, 27, 60);    // dark purple
    private static final TextColor ACCENT     = new TextColor.RGB(102, 126, 234); // blue-purple
    private static final TextColor ACCENT2    = new TextColor.RGB(118, 75, 162);  // purple
    private static final TextColor SUCCESS    = new TextColor.RGB(16, 185, 129);  // green
    private static final TextColor DANGER     = new TextColor.RGB(246, 79, 89);   // red
    private static final TextColor TEXT       = new TextColor.RGB(229, 231, 235); // light grey
    private static final TextColor MUTED      = new TextColor.RGB(107, 114, 128); // grey

    @Override
    public Integer call() {
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            factory.setInitialTerminalSize(new TerminalSize(120, 40));

            Terminal terminal = factory.createTerminal();
            Screen screen = new TerminalScreen(terminal);
            screen.startScreen();

            MultiWindowTextGUI gui = new MultiWindowTextGUI(
                screen,
                new DefaultWindowManager(),
                new EmptySpace(BG)
            );

            if (!sessionManager.isLoggedIn()) {
                showAuthWindow(gui);
            }

            if (sessionManager.isLoggedIn()) {
                showMainWindow(gui);
            }

            screen.stopScreen();
            return 0;

        } catch (Exception e) {
            log.error("TUI error", e);
            System.err.println("TUI error: " + e.getMessage());
            return 1;
        }
    }

    // ── AUTH WINDOW ───────────────────────────────────────────────────────────
    private void showAuthWindow(MultiWindowTextGUI gui) throws IOException {
        AtomicBoolean authenticated = new AtomicBoolean(false);

        while (!authenticated.get()) {
            BasicWindow window = new BasicWindow("  ☁️  CloudCLI — Backup Manager  ");
            window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));

            Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
            root.setFillColorOverride(PANEL_BG);

            // Logo
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
            root.addComponent(label("  ╔══════════════════════════════════════════╗  ", ACCENT, true));
            root.addComponent(label("  ║       ☁️   CloudCLI Backup Manager       ║  ", ACCENT, true));
            root.addComponent(label("  ║         Multi-Database Backup Tool       ║  ", MUTED, false));
            root.addComponent(label("  ╚══════════════════════════════════════════╝  ", ACCENT, true));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
            root.addComponent(label("  Welcome! Please login or create an account.  ", TEXT, false));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            // Buttons panel
            Panel btnPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            btnPanel.setFillColorOverride(PANEL_BG);

            Button loginBtn = new Button("  🔐 Login  ", () -> {
                if (handleLogin(gui)) {
                    authenticated.set(true);
                    window.close();
                }
            });

            Button registerBtn = new Button("  📝 Register  ", () -> {
                if (handleRegister(gui)) {
                    authenticated.set(true);
                    window.close();
                }
            });

            Button exitBtn = new Button("  🚪 Exit  ", () -> {
                authenticated.set(true); // exit loop
                window.close();
            });

            btnPanel.addComponent(loginBtn);
            btnPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
            btnPanel.addComponent(registerBtn);
            btnPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
            btnPanel.addComponent(exitBtn);

            root.addComponent(btnPanel);
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            // Footer
            root.addComponent(label("  Developed by Saqlain Zarjis Ansari  |  +91 8442883695  ", MUTED, false));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            window.setComponent(root);
            gui.addWindowAndWait(window);

            if (!sessionManager.isLoggedIn() && !authenticated.get()) {
                break;
            }
        }
    }

    // ── MAIN WINDOW ───────────────────────────────────────────────────────────
    private void showMainWindow(MultiWindowTextGUI gui) throws IOException {
        AtomicBoolean running = new AtomicBoolean(true);

        while (running.get()) {
            String username = sessionManager.getCurrentUsername();

            BasicWindow window = new BasicWindow("  ☁️  CloudCLI  —  " + username + "  ");
            window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));

            Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
            root.setFillColorOverride(PANEL_BG);

            // Header
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
            root.addComponent(label("  ╔══════════════════════════════════════════════════════╗  ", ACCENT, true));
            root.addComponent(label("  ║              ☁️  CloudCLI Backup Manager              ║  ", ACCENT, true));
            root.addComponent(label("  ║  Logged in as: " + padRight(username, 38) + "║  ", TEXT, false));
            root.addComponent(label("  ╚══════════════════════════════════════════════════════╝  ", ACCENT, true));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            // Menu title
            root.addComponent(label("  MAIN MENU", ACCENT, true));
            root.addComponent(label("  ─────────────────────────────────────────────────────  ", MUTED, false));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            // Menu buttons in 2 columns
            Panel menuGrid = new Panel(new GridLayout(2));
            menuGrid.setFillColorOverride(PANEL_BG);

            menuGrid.addComponent(menuButton("  💻  Command Shell      ", "Quick commands & TAB completion", () -> {
                window.close();
                showCommandShellInfo(gui);
            }));

            menuGrid.addComponent(menuButton("  🎨  Interactive Wizard  ", "Step-by-step guided backup", () -> {
                window.close();
                showWizardInfo(gui);
            }));

            menuGrid.addComponent(menuButton("  📋  My Backups         ", "View all your backups", () -> {
                window.close();
                showBackupsWindow(gui);
            }));

            menuGrid.addComponent(menuButton("  👤  Account Info       ", "View your account details", () -> {
                window.close();
                showAccountWindow(gui);
            }));

            menuGrid.addComponent(menuButton("  ⚙️   Settings           ", "Configure storage & notifications", () -> {
                showInfo(gui, "Settings", "Edit your .env file to configure:\n\n" +
                    "  BACKBLAZE_ACCESS_KEY=your-key\n" +
                    "  BACKBLAZE_SECRET_KEY=your-secret\n" +
                    "  DATABASE_URL=your-supabase-url\n" +
                    "  EMAIL_ENABLED=true\n" +
                    "  EMAIL_PASSWORD=your-app-password");
            }));

            menuGrid.addComponent(menuButton("  🚪  Logout & Exit       ", "Sign out and close CloudCLI", () -> {
                sessionManager.logout();
                running.set(false);
                window.close();
            }));

            root.addComponent(menuGrid);
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            // Footer
            root.addComponent(label("  ─────────────────────────────────────────────────────  ", MUTED, false));
            root.addComponent(label("  Use ↑↓ to navigate  |  Enter to select  |  Tab to switch  ", MUTED, false));
            root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

            window.setComponent(root);
            gui.addWindowAndWait(window);
        }
    }

    // ── BACKUPS WINDOW ────────────────────────────────────────────────────────
    private void showBackupsWindow(MultiWindowTextGUI gui) {
        BasicWindow window = new BasicWindow("  📋  My Backups  ");
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.setFillColorOverride(PANEL_BG);

        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        root.addComponent(label("  YOUR BACKUPS", ACCENT, true));
        root.addComponent(label("  ─────────────────────────────────────────────────────────────  ", MUTED, false));
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        String userId = sessionManager.getCurrentUserId();
        List<BackupRecord> backups = backupRepository.findByUserId(userId);

        if (backups.isEmpty()) {
            root.addComponent(label("  No backups found. Create your first backup!", MUTED, false));
        } else {
            // Table header
            root.addComponent(label(
                String.format("  %-20s  %-10s  %-10s  %-20s",
                    "DATABASE", "TYPE", "SIZE", "DATE"), ACCENT, true));
            root.addComponent(label("  ─────────────────────────────────────────────────────────────  ", MUTED, false));

            for (BackupRecord b : backups) {
                String size = formatSize(b.getSizeBytes());
                String date = DATE_FMT.format(b.getTimestamp());
                root.addComponent(label(
                    String.format("  %-20s  %-10s  %-10s  %-20s",
                        truncate(b.getDatabaseName(), 20),
                        b.getBackupType(),
                        size,
                        date), TEXT, false));
            }
        }

        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        root.addComponent(label("  Total: " + backups.size() + " backup(s)", SUCCESS, false));
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        Button closeBtn = new Button("  ← Back to Menu  ", window::close);
        root.addComponent(closeBtn);
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        window.setComponent(root);
        gui.addWindowAndWait(window);
    }

    // ── ACCOUNT WINDOW ────────────────────────────────────────────────────────
    private void showAccountWindow(MultiWindowTextGUI gui) {
        BasicWindow window = new BasicWindow("  👤  Account Info  ");
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.setFillColorOverride(PANEL_BG);

        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        root.addComponent(label("  ACCOUNT INFORMATION", ACCENT, true));
        root.addComponent(label("  ─────────────────────────────────────────  ", MUTED, false));
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        sessionManager.getCurrentUser().ifPresent(user -> {
            root.addComponent(label("  👤  Username  :  " + user.getUsername(), TEXT, false));
            root.addComponent(label("  📧  Email     :  " + user.getEmail(), TEXT, false));
            root.addComponent(label("  📅  Joined    :  " + DATE_FMT.format(user.getCreatedAt()), TEXT, false));
            root.addComponent(label("  🔑  User ID   :  " + user.getUserId(), MUTED, false));
        });

        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        root.addComponent(label("  ─────────────────────────────────────────  ", MUTED, false));
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        Button closeBtn = new Button("  ← Back to Menu  ", window::close);
        root.addComponent(closeBtn);
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        window.setComponent(root);
        gui.addWindowAndWait(window);
    }

    // ── COMMAND SHELL INFO ────────────────────────────────────────────────────
    private void showCommandShellInfo(MultiWindowTextGUI gui) {
        showInfo(gui, "💻 Command Shell",
            "The Command Shell gives you direct access to all commands.\n\n" +
            "Available commands:\n\n" +
            "  backup --type=sqlite --database=mydb.db\n" +
            "  backup --type=postgres --database=mydb --host=localhost\n" +
            "  list                    Show your backups\n" +
            "  download --id <id>      Download a backup\n" +
            "  whoami                  Show current user\n" +
            "  test-connection         Test database connection\n\n" +
            "Run: cloudcli shell\n" +
            "Or:  cloudcli <command> --help");
    }

    // ── WIZARD INFO ───────────────────────────────────────────────────────────
    private void showWizardInfo(MultiWindowTextGUI gui) {
        showInfo(gui, "🎨 Interactive Wizard",
            "The Interactive Wizard guides you step-by-step.\n\n" +
            "Features:\n\n" +
            "  1. Create New Backup\n" +
            "  2. View Backup History\n" +
            "  3. Test Database Connection\n" +
            "  4. Download Backup\n" +
            "  5. Account Info\n\n" +
            "Run: cloudcli interactive");
    }

    // ── LOGIN HANDLER ─────────────────────────────────────────────────────────
    private boolean handleLogin(MultiWindowTextGUI gui) {
        String username = TextInputDialog.showDialog(gui, "🔐 Login", "Username:", "");
        if (username == null || username.isBlank()) return false;

        String password = TextInputDialog.showPasswordDialog(gui, "🔐 Login", "Password:", "");
        if (password == null || password.isBlank()) return false;

        try {
            User user = userService.login(username, password)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            sessionManager.login(user);
            showSuccess(gui, "✅ Login Successful!", "Welcome back, " + user.getUsername() + "!");
            return true;
        } catch (Exception e) {
            showError(gui, "❌ Login Failed", e.getMessage());
            return false;
        }
    }

    // ── REGISTER HANDLER ──────────────────────────────────────────────────────
    private boolean handleRegister(MultiWindowTextGUI gui) {
        String email = TextInputDialog.showDialog(gui, "📝 Register", "Email:", "");
        if (email == null || email.isBlank()) return false;

        String username = TextInputDialog.showDialog(gui, "📝 Register", "Username:", "");
        if (username == null || username.isBlank()) return false;

        String password = TextInputDialog.showPasswordDialog(gui, "📝 Register", "Password:", "");
        if (password == null || password.isBlank()) return false;

        String confirm = TextInputDialog.showPasswordDialog(gui, "📝 Register", "Confirm Password:", "");
        if (!password.equals(confirm)) {
            showError(gui, "❌ Error", "Passwords do not match!");
            return false;
        }

        try {
            User user = userService.register(email, username, password);
            sessionManager.login(user);
            showSuccess(gui, "✅ Registration Successful!",
                "Welcome, " + user.getUsername() + "!\n\nA welcome email has been sent to:\n" + user.getEmail());
            return true;
        } catch (Exception e) {
            showError(gui, "❌ Registration Failed", e.getMessage());
            return false;
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private Label label(String text, TextColor color, boolean bold) {
        Label l = new Label(text);
        l.setForegroundColor(color);
        if (bold) l.addStyle(SGR.BOLD);
        return l;
    }

    private Panel menuButton(String title, String subtitle, Runnable action) {
        Panel p = new Panel(new LinearLayout(Direction.VERTICAL));
        p.setFillColorOverride(new TextColor.RGB(40, 37, 75));

        Button btn = new Button(title, action);
        Label sub = new Label("  " + subtitle);
        sub.setForegroundColor(MUTED);

        p.addComponent(btn);
        p.addComponent(sub);
        p.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        return p;
    }

    private void showInfo(MultiWindowTextGUI gui, String title, String message) {
        MessageDialog.showMessageDialog(gui, "  ℹ️  " + title + "  ", message, MessageDialogButton.OK);
    }

    private void showSuccess(MultiWindowTextGUI gui, String title, String message) {
        MessageDialog.showMessageDialog(gui, "  " + title + "  ", message, MessageDialogButton.OK);
    }

    private void showError(MultiWindowTextGUI gui, String title, String message) {
        MessageDialog.showMessageDialog(gui, "  " + title + "  ", message, MessageDialogButton.OK);
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
