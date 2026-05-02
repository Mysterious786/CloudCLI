package com.example.cloudcli.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseConfig {

    private String type; // mysql, postgres,mongodb,sqlite
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
}
