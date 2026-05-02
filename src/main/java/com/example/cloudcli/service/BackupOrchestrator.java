package com.example.cloudcli.service;

import com.example.cloudcli.exception.BackupException;
import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.BackupRepository;
import com.example.cloudcli.service.backup.BackupStrategy;
import com.example.cloudcli.service.backup.BackupStrategyFactory;
import com.example.cloudcli.service.connection.ConnectionTester;
import com.example.cloudcli.service.connection.ConnectionTesterFactory;
import com.example.cloudcli.service.notification.NotificationService;
import com.example.cloudcli.service.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Template Method Pattern - Orchestrates the backup workflow
 * Facade Pattern - Simplifies interaction between subsystems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupOrchestrator {
    
    private final BackupStrategyFactory backupStrategyFactory;
    private final ConnectionTesterFactory connectionTesterFactory;
    private final StorageProvider storageProvider;
    private final CompressionService compressionService;
    private final NotificationService notificationService;
    private final BackupRepository backupRepository;
    private final SessionManager sessionManager;
    
    @Value("${backup.temp-dir}")
    private String tempDirPath;
    
    /**
     * Template method defining the backup workflow
     */
    public BackupResult performBackup(DatabaseConfig config, BackupType type, boolean compress) {
        log.info("Starting backup for database: {} (type: {})", config.getDatabase(), type);
        
        try {
            // Step 1: Test connection
            testConnection(config);
            
            // Step 2: Create temp directory
            Path tempDir = createTempDirectory();
            
            // Step 3: Execute backup
            BackupResult result = executeBackup(config, type, tempDir);
            
            // Step 4: Compress if requested
            if (compress) {
                result = compressBackup(result);
            }
            
            // Step 5: Store backup
            result = storeBackup(result);
            
            // Step 6: Save metadata
            saveBackupRecord(result);
            
            // Step 7: Notify success
            notificationService.notifySuccess(result);
            
            log.info("Backup completed successfully: {}", result.getStorageLocation());
            return result;
            
        } catch (Exception e) {
            log.error("Backup failed", e);
            notificationService.notifyFailure("Backup failed for " + config.getDatabase(), e);
            throw new BackupException("Backup failed: " + e.getMessage(), e);
        }
    }
    
    private void testConnection(DatabaseConfig config) {
        log.info("Testing database connection...");
        ConnectionTester tester = connectionTesterFactory.getTester(config.getType());
        boolean connected = tester.test(config);
        
        if (!connected) {
            throw new BackupException("Connection test failed for " + config.getType());
        }
        
        log.info("Connection test successful");
    }
    
    private Path createTempDirectory() {
        try {
            Path tempDir = Paths.get(tempDirPath);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            return tempDir;
        } catch (Exception e) {
            throw new BackupException("Failed to create temp directory", e);
        }
    }
    
    private BackupResult executeBackup(DatabaseConfig config, BackupType type, Path tempDir) {
        log.info("Executing backup strategy for {}", config.getType());
        BackupStrategy strategy = backupStrategyFactory.getStrategy(config.getType());
        return strategy.execute(config, type, tempDir);
    }
    
    private BackupResult compressBackup(BackupResult result) {
        try {
            log.info("Compressing backup...");
            Path originalFile = Paths.get(result.getFilePath());
            Path compressedFile = compressionService.compress(originalFile);
            
            result.setFilePath(compressedFile.toString());
            result.setSizeInBytes(Files.size(compressedFile));
            
            log.info("Compression completed. New size: {} bytes", result.getSizeInBytes());
            return result;
            
        } catch (Exception e) {
            throw new BackupException("Compression failed", e);
        }
    }
    
    private BackupResult storeBackup(BackupResult result) {
        try {
            log.info("Storing backup...");
            Path backupFile = Paths.get(result.getFilePath());
            String filename = backupFile.getFileName().toString();
            
            // Include userId in destination path for multi-user support
            String userId = sessionManager.getCurrentUserId();
            String destination;
            if (userId != null) {
                destination = userId + "/" + filename;
            } else {
                destination = "anonymous/" + filename;
            }
            
            storageProvider.store(backupFile, destination);
            result.setStorageLocation(destination);
            
            // Clean up temp file or directory
            if (Files.isDirectory(backupFile)) {
                deleteDirectory(backupFile);
            } else {
                Files.deleteIfExists(backupFile);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new BackupException("Storage failed", e);
        }
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path, e);
                    }
                });
        }
    }
    
    private void saveBackupRecord(BackupResult result) {
        BackupRecord record = new BackupRecord();
        record.setUserId(sessionManager.getCurrentUserId());  // Add userId
        record.setDatabaseName(result.getDatabaseName());
        record.setBackupType(result.getBackupType().name());
        record.setFilePath(result.getStorageLocation());
        record.setSizeBytes(result.getSizeInBytes());
        record.setTimestamp(result.getTimestamp());
        record.setDurationSeconds(result.getDurationSeconds());
        
        backupRepository.save(record);
        log.info("Backup record saved");
    }
}
