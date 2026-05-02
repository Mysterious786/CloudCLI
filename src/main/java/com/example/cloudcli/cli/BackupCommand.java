package com.example.cloudcli.cli;

import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.service.BackupOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Backup command - performs database backups
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "backup",
    description = "Perform a database backup",
    mixinStandardHelpOptions = true
)
public class BackupCommand implements Callable<Integer> {
    
    private final BackupOrchestrator backupOrchestrator;
    
    @Option(names = {"-t", "--type"}, description = "Database type: mysql, postgres, mongodb, sqlite, supabase", required = true)
    private String dbType;
    
    @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
    private String host;
    
    @Option(names = {"-p", "--port"}, description = "Database port")
    private Integer port;
    
    @Option(names = {"-d", "--database"}, description = "Database name or path (for SQLite)", required = true)
    private String database;
    
    @Option(names = {"-u", "--username"}, description = "Database username")
    private String username;
    
    @Option(names = {"-P", "--password"}, description = "Database password", interactive = true)
    private String password;
    
    @Option(names = {"-b", "--backup-type"}, description = "Backup type: FULL, SCHEMA_ONLY, DATA_ONLY", defaultValue = "FULL")
    private BackupType backupType;
    
    @Option(names = {"-c", "--compress"}, description = "Compress backup file", defaultValue = "true")
    private boolean compress;
    
    @Override
    public Integer call() {
        try {
            log.info("Starting backup for {} database: {}", dbType, database);
            
            DatabaseConfig config = createDatabaseConfig();
            BackupResult result = backupOrchestrator.performBackup(config, backupType, compress);
            
            System.out.println("\n✅ Backup completed successfully!");
            System.out.println("   Database: " + result.getDatabaseName());
            System.out.println("   Type: " + result.getBackupType());
            System.out.println("   Size: " + formatSize(result.getSizeInBytes()));
            System.out.println("   Duration: " + result.getDurationSeconds() + " seconds");
            System.out.println("   Location: " + result.getStorageLocation());
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("\n❌ Backup failed: " + e.getMessage());
            log.error("Backup failed", e);
            return 1;
        }
    }
    
    private DatabaseConfig createDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType(dbType.toLowerCase());
        config.setHost(host);
        config.setPort(port != null ? port : getDefaultPort(dbType));
        config.setDatabase(database);
        config.setUsername(username);
        config.setPassword(password);
        return config;
    }
    
    private int getDefaultPort(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql" -> 3306;
            case "postgres", "supabase" -> 5432;
            case "mongodb" -> 27017;
            default -> 0;
        };
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
