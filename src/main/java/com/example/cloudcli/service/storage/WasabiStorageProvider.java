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
 * Wasabi storage provider (S3-compatible)
 * Cheaper than AWS S3, no egress fees
 */
@Slf4j
@Component("wasabiStorageProvider")
@ConditionalOnProperty(prefix = "backup.storage.wasabi", name = "enabled", havingValue = "true")
public class WasabiStorageProvider implements StorageProvider {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public WasabiStorageProvider(
        @Value("${backup.storage.wasabi.bucket}") String bucketName,
        @Value("${backup.storage.wasabi.region}") String region,
        @Value("${backup.storage.wasabi.access-key}") String accessKey,
        @Value("${backup.storage.wasabi.secret-key}") String secretKey,
        @Value("${backup.storage.wasabi.endpoint}") String endpoint
    ) {
        this.bucketName = bucketName;
        
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .build();
        
        log.info("Initialized Wasabi storage provider for bucket: {}", bucketName);
    }
    
    @Override
    public void store(Path localFile, String destination) throws StorageException {
        try {
            log.info("Uploading to Wasabi: {} -> {}", localFile, destination);
            
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(destination)
                .build();
            
            s3Client.putObject(request, RequestBody.fromFile(localFile));
            log.info("Successfully uploaded to Wasabi: {}", destination);
            
        } catch (Exception e) {
            throw new StorageException("Failed to upload to Wasabi: " + e.getMessage(), e);
        }
    }
    
    @Override
    public InputStream retrieve(String destination) throws StorageException {
        try {
            log.info("Downloading from Wasabi: {}", destination);
            
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(destination)
                .build();
            
            return s3Client.getObject(request);
            
        } catch (Exception e) {
            throw new StorageException("Failed to download from Wasabi: " + e.getMessage(), e);
        }
    }
}
