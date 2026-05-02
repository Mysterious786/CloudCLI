package com.example.cloudcli.service.connection;

import com.example.cloudcli.model.DatabaseConfig;

public interface ConnectionTester {
    boolean test(DatabaseConfig config);
}
