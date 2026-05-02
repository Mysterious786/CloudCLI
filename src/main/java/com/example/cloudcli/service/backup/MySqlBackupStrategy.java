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

@Slf4j
@Component("mysqlBackupStrategy")
@RequiredArgsConstructor
public class MySqlBackupStrategy implements BackupStrategy {

    private final ProcessRunner processRunner;

    @Override
    public BackupResult execute(DatabaseConfig config, BackupType type, Path tempDir) {
        long startTime = System.currentTimeMillis();
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s_%s.sql", config.getDatabase(), type.name().toLowerCase(), timestamp);
            Path backupFile = tempDir.resolve(filename);
            
            List<String> command = buildCommand(config, type, backupFile);
            
            log.info("Executing MySQL backup: {}", String.join(" ", command));
            processRunner.execute(command, config.getPassword());
            
            if (!Files.exists(backupFile) || Files.size(backupFile) == 0) {
                throw new BackupException("Backup file was not created or is empty");
            }
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            long size = Files.size(backupFile);
            
            return BackupResult.builder()
                .databaseName(config.getDatabase())
                .backupType(type)
                .filePath(backupFile.toString())
                .sizeInBytes(size)
                .timestamp(Instant.now())
                .durationSeconds(duration)
                .build();
                
        } catch (Exception e) {
            throw new BackupException("MySQL backup failed: " + e.getMessage(), e);
        }
    }
    
    private List<String> buildCommand(DatabaseConfig config, BackupType type, Path outputFile) {
        List<String> command = new ArrayList<>();
        command.add("mysqldump");
        command.add("-h");
        command.add(config.getHost());
        command.add("-P");
        command.add(String.valueOf(config.getPort()));
        command.add("-u");
        command.add(config.getUsername());
        
        // Common options
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        
        // Backup type specific options
        switch (type) {
            case FULL -> {
                command.add("--databases");
                command.add(config.getDatabase());
            }
            case SCHEMA_ONLY -> {
                command.add("--no-data");
                command.add("--databases");
                command.add(config.getDatabase());
            }
            case DATA_ONLY -> {
                command.add("--no-create-info");
                command.add("--databases");
                command.add(config.getDatabase());
            }
        }
        
        command.add("--result-file=" + outputFile.toString());
        
        return command;
    }
}
