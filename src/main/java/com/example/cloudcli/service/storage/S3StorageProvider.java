package com.example.cloudcli.service.storage;

import com.example.cloudcli.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * AWS S3 storage provider
 */
@Slf4j
@Component("s3StorageProvider")
@ConditionalOnProperty(prefix = "backup.storage.s3", name = "enabled", havingValue = "true")
public class S3StorageProvider implements StorageProvider {
    
    private final S3Client s3Client;
    private final String bucket;

    public S3StorageProvider(
        S3Client s3Client,
        @Value("${backup.storage.s3.bucket}") String bucket
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        log.info("Initialized S3 storage provider for bucket: {}", bucket);
    }

    @Override
    public void store(Path localFile, String destination) throws StorageException {
        try {
            log.info("Uploading to S3: {} -> s3://{}/{}", localFile, bucket, destination);
            
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(destination)
                .build();
            
            s3Client.putObject(request, RequestBody.fromFile(localFile));
            log.info("Successfully uploaded to S3: {}", destination);
            
        } catch (Exception e) {
            throw new StorageException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream retrieve(String destination) throws StorageException {
        try {
            log.info("Downloading from S3: s3://{}/{}", bucket, destination);
            
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(destination)
                .build();
            
            return s3Client.getObject(request);
            
        } catch (Exception e) {
            throw new StorageException("S3 download failed: " + e.getMessage(), e);
        }
    }
}
