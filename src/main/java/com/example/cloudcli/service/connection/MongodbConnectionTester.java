package com.example.cloudcli.service.connection;

import com.example.cloudcli.exception.ConnectionException;
import com.example.cloudcli.model.DatabaseConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB connection tester
 */
@Slf4j
@Component("mongodbConnectionTester")
public class MongodbConnectionTester implements ConnectionTester {
    
    @Override
    public boolean test(DatabaseConfig config) throws ConnectionException {
        try {
            String connectionString;
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                connectionString = String.format("mongodb://%s:%s@%s:%d/%s?authSource=admin&ssl=true&retryWrites=true&w=majority",
                    config.getUsername(),
                    config.getPassword(),
                    config.getHost(),
                    config.getPort(),
                    config.getDatabase()
                );
            } else {
                connectionString = String.format("mongodb://%s:%d/%s?ssl=true",
                    config.getHost(),
                    config.getPort(),
                    config.getDatabase()
                );
            }
            
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToSocketSettings(builder ->
                    builder.connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                )
                .build();
            
            try (MongoClient mongoClient = MongoClients.create(settings)) {
                // Ping the database
                Document ping = mongoClient.getDatabase(config.getDatabase())
                    .runCommand(new Document("ping", 1));
                
                // MongoDB returns "ok" as either Integer or Double depending on version
                Number okValue = ping.get("ok", Number.class);
                boolean success = okValue != null && okValue.doubleValue() == 1.0;
                log.info("MongoDB connection test: {}", success ? "SUCCESS" : "FAILED");
                return success;
            }
            
        } catch (Exception e) {
            throw new ConnectionException("MongoDB connection failed: " + e.getMessage(), e);
        }
    }
}
