package com.example.cloudcli.service.storage;

import com.example.cloudcli.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 * Backblaze B2 storage provider (S3-compatible)
 * Free tier: 10GB storage, 1GB daily download
 */
@Slf4j
@Component("backblazeStorageProvider")
@ConditionalOnProperty(prefix = "backup.storage.backblaze", name = "enabled", havingValue = "true")
public class BackblazeStorageProvider implements StorageProvider {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public BackblazeStorageProvider(
        @Value("${backup.storage.backblaze.bucket}") String bucketName,
        @Value("${backup.storage.backblaze.region}") String region,
        @Value("${backup.storage.backblaze.access-key}") String accessKey,
        @Value("${backup.storage.backblaze.secret-key}") String secretKey,
        @Value("${backup.storage.backblaze.endpoint}") String endpoint
    ) {
        this.bucketName = bucketName;
        
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .build();
        
        log.info("Initialized Backblaze B2 storage provider for bucket: {}", bucketName);
    }
    
    @Override
    public void store(Path localFile, String destination) throws StorageException {
        try {
            log.info("Uploading to Backblaze B2: {} -> {}", localFile, destination);
            
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(destination)
                .build();
            
            s3Client.putObject(request, RequestBody.fromFile(localFile));
            log.info("Successfully uploaded to Backblaze B2: {}", destination);
            
        } catch (Exception e) {
            throw new StorageException("Failed to upload to Backblaze B2: " + e.getMessage(), e);
        }
    }
    
    @Override
    public InputStream retrieve(String destination) throws StorageException {
        try {
            log.info("Downloading from Backblaze B2: {}", destination);
            
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(destination)
                .build();
            
            return s3Client.getObject(request);
            
        } catch (Exception e) {
            throw new StorageException("Failed to download from Backblaze B2: " + e.getMessage(), e);
        }
    }
}
