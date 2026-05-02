package com.example.cloudcli.cli;

import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Download command - downloads backup from cloud storage
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "download",
    description = "Download backup from cloud storage",
    mixinStandardHelpOptions = true
)
public class DownloadCommand implements Callable<Integer> {
    
    private final StorageProvider storageProvider;
    private final InMemoryBackupRepository backupRepository;
    private final SessionManager sessionManager;
    
    @Option(names = {"-i", "--id"}, description = "Backup ID (from 'list' command)")
    private String backupId;
    
    @Option(names = {"-f", "--file"}, description = "Remote file path (e.g., userId/backup.db)")
    private String remoteFile;
    
    @Option(names = {"-o", "--output"}, description = "Output directory", defaultValue = "./downloads")
    private String outputDir;
    
    @Option(names = {"-n", "--name"}, description = "Output filename (optional)")
    private String outputName;
    
    @Override
    public Integer call() {
        try {
            // Check if user is logged in
            if (!sessionManager.isLoggedIn()) {
                System.err.println("❌ You must be logged in to download backups");
                System.err.println("   Use: cloudcli login -u <username>");
                return 1;
            }
            
            String userId = sessionManager.getCurrentUserId();
            String username = sessionManager.getCurrentUsername();
            
            System.out.println("\n⬇️  Downloading backup...");
            System.out.println("   User: " + username);
            
            // Determine which backup to download
            String fileToDownload;
            String fileName;
            
            if (backupId != null) {
                // Download by backup ID
                BackupRecord backup = backupRepository.findById(backupId)
                    .orElseThrow(() -> new IllegalArgumentException("Backup not found: " + backupId));
                
                // Verify backup belongs to current user
                if (!backup.getUserId().equals(userId)) {
                    System.err.println("❌ Access denied: This backup belongs to another user");
                    return 1;
                }
                
                fileToDownload = backup.getFilePath();
                fileName = Paths.get(fileToDownload).getFileName().toString();
                
                System.out.println("   Backup ID: " + backupId);
                System.out.println("   Database: " + backup.getDatabaseName());
                System.out.println("   Type: " + backup.getBackupType());
                System.out.println("   Size: " + formatSize(backup.getSizeBytes()));
                
            } else if (remoteFile != null) {
                // Download by file path
                // Verify file path starts with user's ID (security check)
                if (!remoteFile.startsWith(userId + "/")) {
                    System.err.println("❌ Access denied: You can only download your own backups");
                    System.err.println("   Your backups are in: " + userId + "/");
                    return 1;
                }
                
                fileToDownload = remoteFile;
                fileName = Paths.get(remoteFile).getFileName().toString();
                
            } else {
                // Show available backups
                System.err.println("❌ Please specify either --id or --file");
                System.err.println("\nAvailable backups:");
                
                List<BackupRecord> backups = backupRepository.findByUserId(userId);
                if (backups.isEmpty()) {
                    System.err.println("   No backups found");
                } else {
                    for (int i = 0; i < backups.size(); i++) {
                        BackupRecord b = backups.get(i);
                        System.err.println(String.format("   %d. %s (%s, %s)",
                            i + 1, b.getDatabaseName(), b.getBackupType(), formatSize(b.getSizeBytes())));
                        System.err.println("      ID: " + b.getId());
                        System.err.println("      File: " + b.getFilePath());
                    }
                }
                return 1;
            }
            
            // Determine output path
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);
            
            String finalFileName = outputName != null ? outputName : fileName;
            Path outputFile = outputPath.resolve(finalFileName);
            
            System.out.println("   Remote: " + fileToDownload);
            System.out.println("   Local: " + outputFile.toAbsolutePath());
            System.out.println();
            
            // Download file
            long startTime = System.currentTimeMillis();
            
            try (InputStream inputStream = storageProvider.retrieve(fileToDownload);
                 FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                long duration = (System.currentTimeMillis() - startTime) / 1000;
                
                System.out.println("✅ Download completed successfully!");
                System.out.println("   Size: " + formatSize(totalBytes));
                System.out.println("   Duration: " + duration + "s");
                System.out.println("   Location: " + outputFile.toAbsolutePath());
                
                log.info("Downloaded backup: {} -> {}", fileToDownload, outputFile);
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            log.error("Download failed", e);
            return 1;
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
