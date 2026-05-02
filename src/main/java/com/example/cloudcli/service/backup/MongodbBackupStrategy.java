package com.example.cloudcli.service.backup;

import com.example.cloudcli.exception.BackupException;
import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.util.ProcessRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB backup strategy using mongodump
 */
@Slf4j
@Component("mongodbBackupStrategy")
@RequiredArgsConstructor
public class MongodbBackupStrategy implements BackupStrategy {
    
    private final ProcessRunner processRunner;
    
    @Override
    public BackupResult execute(DatabaseConfig config, BackupType type, Path tempDir) {
        long startTime = System.currentTimeMillis();
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dirname = String.format("%s_%s_%s", config.getDatabase(), type.name().toLowerCase(), timestamp);
            Path backupDir = tempDir.resolve(dirname);
            
            List<String> command = buildCommand(config, type, backupDir);
            
            log.info("Executing MongoDB backup: {}", String.join(" ", command));
            processRunner.execute(command, null);
            
            if (!Files.exists(backupDir) || !Files.isDirectory(backupDir)) {
                throw new BackupException("Backup directory was not created");
            }
            
            // Calculate total size of backup directory
            long size = Files.walk(backupDir)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            
            return BackupResult.builder()
                .databaseName(config.getDatabase())
                .backupType(type)
                .filePath(backupDir.toString())
                .sizeInBytes(size)
                .timestamp(Instant.now())
                .durationSeconds(duration)
                .build();
                
        } catch (Exception e) {
            throw new BackupException("MongoDB backup failed: " + e.getMessage(), e);
        }
    }
    
    private List<String> buildCommand(DatabaseConfig config, BackupType type, Path outputDir) {
        List<String> command = new ArrayList<>();
        command.add("mongodump");
        
        // Connection string with SSL for MongoDB Atlas
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            String uri = String.format("mongodb://%s:%s@%s:%d/%s?authSource=admin&ssl=true&retryWrites=true&w=majority",
                config.getUsername(),
                config.getPassword(),
                config.getHost(),
                config.getPort(),
                config.getDatabase()
            );
            command.add("--uri=" + uri);
        } else {
            command.add("--host");
            command.add(config.getHost());
            command.add("--port");
            command.add(String.valueOf(config.getPort()));
            command.add("--db");
            command.add(config.getDatabase());
            command.add("--ssl");
        }
        
        command.add("--out");
        command.add(outputDir.toString());
        
        // Backup type specific options
        if (type == BackupType.SCHEMA_ONLY) {
            command.add("--excludeCollection");
            command.add("*");  // This is a workaround; MongoDB doesn't have pure schema-only
        }
        
        return command;
    }
}
