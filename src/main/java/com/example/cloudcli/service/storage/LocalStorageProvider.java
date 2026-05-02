package com.example.cloudcli.service.storage;

import com.example.cloudcli.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * Local filesystem storage provider
 */
@Slf4j
@Component("localStorageProvider")
public class LocalStorageProvider implements StorageProvider {

    @Value("${backup.storage.local.base-path:./backups}")
    private String backupDir;

    @Override
    public void store(Path localFile, String destination) throws StorageException {
        try {
            Path targetDir = Paths.get(backupDir);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(destination);
            
            log.info("Storing backup locally: {} -> {}", localFile, targetPath);
            
            if (Files.isDirectory(localFile)) {
                // Copy directory recursively
                copyDirectory(localFile, targetPath);
            } else {
                // Copy single file
                Files.copy(localFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.info("Successfully stored backup: {}", targetPath);

        } catch (IOException e) {
            throw new StorageException("Local storage failed: " + e.getMessage(), e);
        }
    }
    
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + sourcePath, e);
                }
            });
    }

    @Override
    public InputStream retrieve(String destination) throws StorageException {
        try {
            Path file = Paths.get(backupDir, destination);
            log.info("Retrieving backup from: {}", file);
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new StorageException("File not found: " + destination, e);
        }
    }
}