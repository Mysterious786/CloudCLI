package com.example.cloudcli.model;

public enum BackupType {
    FULL,           // Complete database backup
    SCHEMA_ONLY,    // Only database structure (tables, indexes, etc.)
    DATA_ONLY       // Only data without schema
}
