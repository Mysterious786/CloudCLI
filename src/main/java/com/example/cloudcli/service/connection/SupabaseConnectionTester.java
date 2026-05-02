package com.example.cloudcli.service.connection;

import com.example.cloudcli.exception.ConnectionException;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Supabase connection tester - delegates to PostgreSQL
 */
@Slf4j
@Component("supabaseConnectionTester")
@RequiredArgsConstructor
public class SupabaseConnectionTester implements ConnectionTester {
    
    private final PostgresConnectionTester postgresConnectionTester;
    
    @Override
    public boolean test(DatabaseConfig config) throws ConnectionException {
        log.info("Supabase connection test - using PostgreSQL tester");
        return postgresConnectionTester.test(config);
    }
}
