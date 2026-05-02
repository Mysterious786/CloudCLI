package com.example.cloudcli.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProcessRunner {

    private static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(60);

    /**
     * Execute a command with optional password (for database tools)
     */
    public int execute(List<String> command, String password) throws IOException, InterruptedException {
        return execute(command, password, DEFAULT_TIMEOUT_MS);
    }

    public int execute(List<String> command, String password, long timeoutMs) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        // Set password as environment variable if provided
        if (password != null && !password.isEmpty()) {
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", password);  // PostgreSQL
            env.put("MYSQL_PWD", password);   // MySQL
        }

        Process process = pb.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug(line);
            }
        }

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

        if (!finished) {
            process.destroyForcibly();
            log.error("Command timed out: {}", String.join(" ", command));
            throw new IOException("Command timed out after " + timeoutMs + "ms");
        }

        int exitCode = process.exitValue();
        
        if (exitCode != 0) {
            log.error("Command failed with exit code {}: {}", exitCode, String.join(" ", command));
            log.error("Output: {}", output);
            throw new IOException("Command failed with exit code " + exitCode);
        }

        log.info("Command completed successfully: {}", String.join(" ", command));
        return exitCode;
    }
}