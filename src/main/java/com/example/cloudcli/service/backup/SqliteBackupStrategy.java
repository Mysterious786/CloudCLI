package com.example.cloudcli.service.backup;

import com.example.cloudcli.exception.BackupException;
import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SQLite backup strategy using file copy
 */
@Slf4j
@Component("sqliteBackupStrategy")
public class SqliteBackupStrategy implements BackupStrategy {
    
    @Override
    public BackupResult execute(DatabaseConfig config, BackupType type, Path tempDir) {
        long startTime = System.currentTimeMillis();
        
        try {
            // SQLite uses the 'database' field to store the file path
            Path sourceFile = Path.of(config.getDatabase());
            
            if (!Files.exists(sourceFile)) {
                throw new BackupException("SQLite database file not found: " + sourceFile);
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s_%s.db",
                sourceFile.getFileName().toString().replaceAll("\\.db$", ""),
                type.name().toLowerCase(),
                timestamp
            );
            Path backupFile = tempDir.resolve(filename);
            
            log.info("Copying SQLite database from {} to {}", sourceFile, backupFile);
            
            // For SQLite, we simply copy the file
            // FULL backup = copy entire file
            // SCHEMA_ONLY and DATA_ONLY would require sqlite3 CLI tool
            Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            long size = Files.size(backupFile);
            
            return BackupResult.builder()
                .databaseName(sourceFile.getFileName().toString())
                .backupType(type)
                .filePath(backupFile.toString())
                .sizeInBytes(size)
                .timestamp(Instant.now())
                .durationSeconds(duration)
                .build();
                
        } catch (IOException e) {
            throw new BackupException("SQLite backup failed: " + e.getMessage(), e);
        }
    }
}
