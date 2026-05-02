package com.example.cloudcli.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for BackupRecord entity
 * Provides persistent storage for backup metadata
 */
@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, String> {
    
    List<BackupRecord> findByUserId(String userId);
    
    List<BackupRecord> findByUserIdOrderByTimestampDesc(String userId);
}
