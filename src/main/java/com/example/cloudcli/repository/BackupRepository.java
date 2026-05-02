package com.example.cloudcli.repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Pattern - Interface for backup metadata storage
 */
public interface BackupRepository {
    void save(BackupRecord record);
    Optional<BackupRecord> findById(String id);
    List<BackupRecord> findAll();
    List<BackupRecord> findByDatabaseName(String databaseName);
    void deleteById(String id);
}
