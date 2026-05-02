package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.BackupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Console-based notifier implementation
 */
@Slf4j
@Component
public class ConsoleNotifier implements Notifier {
    
    @Override
    public void notifySuccess(BackupResult result) {
        log.info("✅ Backup completed successfully!");
        log.info("   Database: {}", result.getDatabaseName());
        log.info("   Type: {}", result.getBackupType());
        log.info("   Size: {} MB", result.getSizeInBytes() / (1024.0 * 1024.0));
        log.info("   Duration: {} seconds", result.getDurationSeconds());
        log.info("   Location: {}", result.getStorageLocation());
    }
    
    @Override
    public void notifyFailure(String message, Throwable error) {
        log.error("❌ Backup failed: {}", message);
        if (error != null) {
            log.error("   Error: {}", error.getMessage());
        }
    }
}
