package com.example.cloudcli.service.connection;

import com.example.cloudcli.exception.ConnectionException;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite connection tester
 */
@Slf4j
@Component("sqliteConnectionTester")
public class SqliteConnectionTester implements ConnectionTester {
    
    @Override
    public boolean test(DatabaseConfig config) throws ConnectionException {
        try {
            Path dbPath = Path.of(config.getDatabase());
            
            if (!Files.exists(dbPath)) {
                throw new ConnectionException("SQLite database file not found: " + dbPath);
            }
            
            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            
            try (Connection conn = DriverManager.getConnection(url)) {
                boolean valid = conn.isValid(5);
                log.info("SQLite connection test: {}", valid ? "SUCCESS" : "FAILED");
                return valid;
            }
            
        } catch (SQLException e) {
            throw new ConnectionException("SQLite connection failed: " + e.getMessage(), e);
        }
    }
}
