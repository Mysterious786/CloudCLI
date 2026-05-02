package com.example.cloudcli.service.connection;

import java.util.Map;

/**
 * Factory Pattern - Creates connection testers based on database type
 */
public class ConnectionTesterFactory {
    
    private final Map<String, ConnectionTester> testers;
    
    public ConnectionTesterFactory(Map<String, ConnectionTester> testers) {
        this.testers = testers;
    }
    
    public ConnectionTester getTester(String dbType) {
        String beanName = dbType.toLowerCase() + "ConnectionTester";
        ConnectionTester tester = testers.get(beanName);
        
        if (tester == null) {
            throw new IllegalArgumentException("No connection tester for type: " + dbType);
        }
        
        return tester;
    }
}
