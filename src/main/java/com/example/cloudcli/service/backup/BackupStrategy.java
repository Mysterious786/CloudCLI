package com.example.cloudcli.service.backup;

import com.example.cloudcli.model.BackupResult;
import com.example.cloudcli.model.BackupType;
import com.example.cloudcli.model.DatabaseConfig;

import java.nio.file.Path;

// Strategy + Factory
public interface BackupStrategy {
    BackupResult execute(DatabaseConfig config, BackupType type, Path tempDir);
}
