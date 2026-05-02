package com.example.cloudcli.service.connection;

import com.example.cloudcli.exception.ConnectionException;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * PostgreSQL connection tester
 */
@Slf4j
@Component("postgresConnectionTester")
public class PostgresConnectionTester implements ConnectionTester {
    
    @Override
    public boolean test(DatabaseConfig config) throws ConnectionException {
        String url = String.format("jdbc:postgresql://%s:%d/%s",
            config.getHost(),
            config.getPort(),
            config.getDatabase()
        );
        
        try (Connection conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword())) {
            boolean valid = conn.isValid(5);
            log.info("PostgreSQL connection test: {}", valid ? "SUCCESS" : "FAILED");
            return valid;
        } catch (SQLException e) {
            throw new ConnectionException("PostgreSQL connection failed: " + e.getMessage(), e);
        }
    }
}
