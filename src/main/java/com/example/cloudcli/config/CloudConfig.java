package com.example.cloudcli.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Slf4j
@Configuration
public class CloudConfig {

    /**
     * AWS S3 Client configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "backup.storage.s3", name = "enabled", havingValue = "true")
    public S3Client s3Client(
        @Value("${backup.storage.s3.region}") String region,
        @Value("${backup.storage.s3.access-key}") String accessKey,
        @Value("${backup.storage.s3.secret-key}") String secretKey,
        @Value("${backup.storage.s3.endpoint:}") String endpoint
    ) {
        log.info("Initializing AWS S3 client for region: {}", region);
        
        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ));
        
        // Custom endpoint for S3-compatible services
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
}
