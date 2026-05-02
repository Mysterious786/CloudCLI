package com.example.cloudcli.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupResult {
    private String databaseName;
    private BackupType backupType;
    private String filePath;
    private long sizeInBytes;
    private Instant timestamp;
    private long durationSeconds;
    private String storageLocation;
    private boolean success;
    private String message;
}
