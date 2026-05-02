package com.example.cloudcli.service.ai;

import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Schema Extractor Service
 * Extracts database schema from various database types
 */
@Slf4j
@Service
public class SchemaExtractorService {

    @Value("${backup.tools.pg-dump:/opt/homebrew/opt/postgresql@17/bin/pg_dump}")
    private String pgDumpPath;

    @Value("${backup.tools.mysql-dump:mysqldump}")
    private String mysqlDumpPath;

    /**
     * Extract schema from database
     * Returns SQL DDL statements
     */
    public String extractSchema(DatabaseConfig config) {
        if (config == null || config.getType() == null) {
            throw new IllegalArgumentException("Database configuration is required");
        }

        return switch (config.getType().toLowerCase()) {
            case "postgres", "supabase" -> extractPostgresSchema(config);
            case "mysql"                -> extractMysqlSchema(config);
            case "sqlite"               -> extractSqliteSchema(config);
            case "mongodb"              -> extractMongoSchema(config);
            default -> throw new IllegalArgumentException(
                "Unsupported database type for schema extraction: " + config.getType()
            );
        };
    }

    // ── PostgreSQL ────────────────────────────────────────────────────────────
    private String extractPostgresSchema(DatabaseConfig config) {
        validateConfig(config, false);

        List<String> cmd = new ArrayList<>();
        cmd.add(pgDumpPath);
        cmd.add("-h"); cmd.add(config.getHost());
        cmd.add("-p"); cmd.add(String.valueOf(config.getPort() > 0 ? config.getPort() : 5432));
        cmd.add("-U"); cmd.add(config.getUsername());
        cmd.add("-d"); cmd.add(config.getDatabase());
        cmd.add("--schema-only");
        cmd.add("--no-owner");
        cmd.add("--no-privileges");
        cmd.add("--no-comments");

        return runCommand(cmd, config.getPassword(), "PostgreSQL schema extraction");
    }

    // ── MySQL ─────────────────────────────────────────────────────────────────
    private String extractMysqlSchema(DatabaseConfig config) {
        validateConfig(config, false);

        List<String> cmd = new ArrayList<>();
        cmd.add(mysqlDumpPath);
        cmd.add("-h"); cmd.add(config.getHost());
        cmd.add("-P"); cmd.add(String.valueOf(config.getPort() > 0 ? config.getPort() : 3306));
        cmd.add("-u"); cmd.add(config.getUsername());
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            cmd.add("-p" + config.getPassword());
        }
        cmd.add("--no-data");
        cmd.add("--routines");
        cmd.add("--triggers");
        cmd.add(config.getDatabase());

        return runCommand(cmd, null, "MySQL schema extraction");
    }

    // ── SQLite ────────────────────────────────────────────────────────────────
    private String extractSqliteSchema(DatabaseConfig config) {
        if (config.getDatabase() == null || config.getDatabase().isBlank()) {
            throw new IllegalArgumentException("SQLite database file path is required");
        }

        java.io.File dbFile = new java.io.File(config.getDatabase());
        if (!dbFile.exists()) {
            throw new IllegalArgumentException("SQLite database file not found: " + config.getDatabase());
        }

        StringBuilder schema = new StringBuilder();
        String url = "jdbc:sqlite:" + config.getDatabase();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Get all CREATE statements
            ResultSet rs = stmt.executeQuery(
                "SELECT sql FROM sqlite_master WHERE sql IS NOT NULL ORDER BY type, name"
            );

            while (rs.next()) {
                String sql = rs.getString("sql");
                if (sql != null && !sql.isBlank()) {
                    schema.append(sql).append(";\n\n");
                }
            }

            if (schema.isEmpty()) {
                return "-- Empty database: no tables found\n";
            }

            return schema.toString();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to extract SQLite schema: " + e.getMessage(), e);
        }
    }

    // ── MongoDB ───────────────────────────────────────────────────────────────
    private String extractMongoSchema(DatabaseConfig config) {
        // MongoDB doesn't have a fixed schema, so we describe collections
        StringBuilder schema = new StringBuilder();
        schema.append("-- MongoDB Database: ").append(config.getDatabase()).append("\n");
        schema.append("-- Note: MongoDB is schema-less. Showing collection structure.\n\n");

        try {
            String uri = buildMongoUri(config);
            com.mongodb.client.MongoClient mongoClient = com.mongodb.client.MongoClients.create(uri);
            com.mongodb.client.MongoDatabase db = mongoClient.getDatabase(config.getDatabase());

            for (String collectionName : db.listCollectionNames()) {
                schema.append("-- Collection: ").append(collectionName).append("\n");

                // Sample one document to infer schema
                com.mongodb.client.MongoCollection<org.bson.Document> collection =
                    db.getCollection(collectionName);

                org.bson.Document sample = collection.find().first();
                if (sample != null) {
                    schema.append("-- Fields: ");
                    schema.append(String.join(", ", sample.keySet()));
                    schema.append("\n");
                    schema.append("-- Document count: ").append(collection.countDocuments()).append("\n");
                }
                schema.append("\n");
            }

            mongoClient.close();

        } catch (Exception e) {
            schema.append("-- Could not connect to MongoDB: ").append(e.getMessage()).append("\n");
            log.warn("MongoDB schema extraction failed: {}", e.getMessage());
        }

        return schema.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String runCommand(List<String> cmd, String password, String operation) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        if (password != null && !password.isBlank()) {
            pb.environment().put("PGPASSWORD", password);
        }

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();

            // Read stdout
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Read stderr
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = errors.toString().trim();
                throw new RuntimeException(operation + " failed: " + errorMsg);
            }

            if (output.isEmpty()) {
                throw new RuntimeException(operation + " produced no output");
            }

            return output.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to run " + operation + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(operation + " was interrupted", e);
        }
    }

    private void validateConfig(DatabaseConfig config, boolean requireDb) {
        if (config.getHost() == null || config.getHost().isBlank()) {
            throw new IllegalArgumentException("Database host is required");
        }
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            throw new IllegalArgumentException("Database username is required");
        }
        if (requireDb && (config.getDatabase() == null || config.getDatabase().isBlank())) {
            throw new IllegalArgumentException("Database name is required");
        }
    }

    private String buildMongoUri(DatabaseConfig config) {
        if (config.getUsername() != null && !config.getUsername().isBlank()
                && config.getPassword() != null && !config.getPassword().isBlank()) {
            return String.format("mongodb://%s:%s@%s:%d/%s",
                config.getUsername(), config.getPassword(),
                config.getHost(), config.getPort() > 0 ? config.getPort() : 27017,
                config.getDatabase());
        }
        return String.format("mongodb://%s:%d",
            config.getHost(), config.getPort() > 0 ? config.getPort() : 27017);
    }
}
