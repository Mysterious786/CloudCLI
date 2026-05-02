package com.example.cloudcli.service.connection;

import com.example.cloudcli.exception.ConnectionException;
import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component("mysqlConnectionTester")
@Slf4j
public class MySqlConnectionTester implements ConnectionTester {

    @Override
    public boolean test(DatabaseConfig config)
    {
        String url = String.format("jdbc:mysql://%s:%d/%s?connectTimeout=5000",config.getHost(),config.getPort(),config.getDatabase());
        try(Connection conn = DriverManager.getConnection(url,config.getUsername(), config.getPassword()))
        {
            boolean valid = conn.isValid(5);
            log.info("MySQL connection test: {}", valid?"SUCCESS":"FAILED");
            return valid;

        }catch(SQLException e)
        {
            throw new ConnectionException("MySQL connection failed",e);
        }

    }
}
