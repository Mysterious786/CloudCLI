package com.example.cloudcli.config;

import com.example.cloudcli.service.backup.BackupStrategy;
import com.example.cloudcli.service.backup.BackupStrategyFactory;
import com.example.cloudcli.service.connection.ConnectionTester;
import com.example.cloudcli.service.connection.ConnectionTesterFactory;
import com.example.cloudcli.service.storage.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

@Slf4j
@Configuration
public class AppConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("backup-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public BackupStrategyFactory backupStrategyFactory(Map<String, BackupStrategy> strategies) {
        log.info("Registered backup strategies: {}", strategies.keySet());
        return new BackupStrategyFactory(strategies);
    }

    @Bean
    public ConnectionTesterFactory connectionTesterFactory(Map<String, ConnectionTester> testers) {
        log.info("Registered connection testers: {}", testers.keySet());
        return new ConnectionTesterFactory(testers);
    }

    /**
     * Primary storage provider based on configuration
     */
    @Bean
    public StorageProvider storageProvider(
        @Value("${backup.storage.default:local}") String defaultProvider,
        Map<String, StorageProvider> providers
    ) {
        String beanName = defaultProvider + "StorageProvider";
        StorageProvider provider = providers.get(beanName);
        
        if (provider == null) {
            log.warn("Storage provider '{}' not found, falling back to local", defaultProvider);
            provider = providers.get("localStorageProvider");
        }
        
        log.info("Using storage provider: {}", provider.getClass().getSimpleName());
        return provider;
    }
}
