package com.example.cloudcli.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Repository Pattern - Backup metadata record
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "backup_records", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class BackupRecord {
    
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "database_name", nullable = false, length = 255)
    private String databaseName;
    
    @Column(name = "backup_type", nullable = false, length = 50)
    private String backupType;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;
}
