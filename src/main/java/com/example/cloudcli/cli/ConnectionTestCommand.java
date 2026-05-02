package com.example.cloudcli.cli;

import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.service.connection.ConnectionTester;
import com.example.cloudcli.service.connection.ConnectionTesterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Connection test command - tests database connectivity
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "test",
    description = "Test database connection",
    mixinStandardHelpOptions = true
)
public class ConnectionTestCommand implements Callable<Integer> {
    
    private final ConnectionTesterFactory connectionTesterFactory;
    
    @Option(names = {"-t", "--type"}, description = "Database type: mysql, postgres, mongodb, sqlite, supabase", required = true)
    private String dbType;
    
    @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
    private String host;
    
    @Option(names = {"-p", "--port"}, description = "Database port")
    private Integer port;
    
    @Option(names = {"-d", "--database"}, description = "Database name or path (for SQLite)", required = true)
    private String database;
    
    @Option(names = {"-u", "--username"}, description = "Database username")
    private String username;
    
    @Option(names = {"-P", "--password"}, description = "Database password", interactive = true)
    private String password;
    
    @Override
    public Integer call() {
        try {
            System.out.println("\n🔍 Testing connection to " + dbType + " database...");
            
            DatabaseConfig config = createDatabaseConfig();
            ConnectionTester tester = connectionTesterFactory.getTester(dbType.toLowerCase());
            
            boolean success = tester.test(config);
            
            if (success) {
                System.out.println("✅ Connection successful!");
                return 0;
            } else {
                System.out.println("❌ Connection failed!");
                return 1;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Connection test failed: " + e.getMessage());
            log.error("Connection test failed", e);
            return 1;
        }
    }
    
    private DatabaseConfig createDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType(dbType.toLowerCase());
        config.setHost(host);
        config.setPort(port != null ? port : getDefaultPort(dbType));
        config.setDatabase(database);
        config.setUsername(username);
        config.setPassword(password);
        return config;
    }
    
    private int getDefaultPort(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql" -> 3306;
            case "postgres", "supabase" -> 5432;
            case "mongodb" -> 27017;
            default -> 0;
        };
    }
}
