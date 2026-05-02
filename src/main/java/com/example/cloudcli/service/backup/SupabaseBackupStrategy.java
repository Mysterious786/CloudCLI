package com.example.cloudcli.service.backup;

import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Supabase backup strategy - delegates to PostgreSQL since Supabase uses PostgreSQL
 */
@Slf4j
@Component("supabaseBackupStrategy")
@RequiredArgsConstructor
public class SupabaseBackupStrategy implements BackupStrategy {
    
    private final PostgresBackupStrategy postgresBackupStrategy;
    
    @Override
    public BackupResult execute(DatabaseConfig config, BackupType type, Path tempDir) {
        log.info("Supabase backup - using PostgreSQL strategy");
        
        // Supabase is PostgreSQL under the hood, so we delegate to PostgreSQL strategy
        // Just ensure the config has the correct Supabase connection details
        return postgresBackupStrategy.execute(config, type, tempDir);
    }
}
