package com.example.cloudcli.cli;

import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * List user's backups
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "list",
    description = "List your backups",
    mixinStandardHelpOptions = true
)
public class ListBackupsCommand implements Callable<Integer> {
    
    private final InMemoryBackupRepository backupRepository;
    private final SessionManager sessionManager;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    @Override
    public Integer call() {
        if (!sessionManager.isLoggedIn()) {
            System.err.println("❌ You must be logged in to list backups");
            System.err.println("   Run: cloudcli login -u <username> -p <password>");
            return 1;
        }
        
        String userId = sessionManager.getCurrentUserId();
        List<BackupRecord> backups = backupRepository.findByUserId(userId);
        
        if (backups.isEmpty()) {
            System.out.println("No backups found for user: " + sessionManager.getCurrentUsername());
            System.out.println("\nCreate your first backup with:");
            System.out.println("  cloudcli backup --type sqlite --database test.db");
            return 0;
        }
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    YOUR BACKUPS                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("User: " + sessionManager.getCurrentUsername());
        System.out.println("Total backups: " + backups.size());
        System.out.println();
        
        for (int i = 0; i < backups.size(); i++) {
            BackupRecord backup = backups.get(i);
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.println("Backup #" + (i + 1));
            System.out.println("  ID: " + backup.getId());
            System.out.println("  Database: " + backup.getDatabaseName());
            System.out.println("  Type: " + backup.getBackupType());
            System.out.println("  Size: " + formatSize(backup.getSizeBytes()));
            System.out.println("  Created: " + DATE_FORMATTER.format(backup.getTimestamp()));
            System.out.println("  Duration: " + backup.getDurationSeconds() + "s");
            System.out.println("  Location: " + backup.getFilePath());
        }
        System.out.println("─────────────────────────────────────────────────────────────");
        
        return 0;
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
