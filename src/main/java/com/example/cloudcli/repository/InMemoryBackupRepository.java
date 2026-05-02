package com.example.cloudcli.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * JPA-backed implementation of BackupRepository
 * Uses H2 database for persistent storage
 */
@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class InMemoryBackupRepository implements BackupRepository {
    
    private final BackupRecordRepository backupRecordRepository;
    
    @Override
    public void save(BackupRecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        backupRecordRepository.save(record);
        log.debug("Saved backup record: {}", record.getId());
    }
    
    @Override
    public Optional<BackupRecord> findById(String id) {
        return backupRecordRepository.findById(id);
    }
    
    @Override
    public List<BackupRecord> findAll() {
        return backupRecordRepository.findAll();
    }
    
    @Override
    public List<BackupRecord> findByDatabaseName(String databaseName) {
        return backupRecordRepository.findAll().stream()
            .filter(record -> record.getDatabaseName().equals(databaseName))
            .toList();
    }
    
    public List<BackupRecord> findByUserId(String userId) {
        return backupRecordRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    @Override
    public void deleteById(String id) {
        backupRecordRepository.deleteById(id);
        log.debug("Deleted backup record: {}", id);
    }
}
