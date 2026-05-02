package com.example.cloudcli.service.backup;

import java.util.Map;

public class BackupStrategyFactory {
    private final Map<String,BackupStrategy> strategies;

    public BackupStrategyFactory(Map<String, BackupStrategy> strategies) {
        this.strategies = strategies;
    }
    public BackupStrategy getStrategy(String dbType)
    {
        String beanName = dbType.toLowerCase() + "BackupStrategy";
        BackupStrategy strategy = strategies.get(beanName);
        if(strategy == null) throw new IllegalArgumentException("No backup strategy for type: "+ dbType);
        return strategy;
    }
}
