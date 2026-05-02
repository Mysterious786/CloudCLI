package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.BackupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composite notifier that delegates to all registered notifiers
 */
@Slf4j
@Service
public class NotificationService implements Notifier {
    
    private final List<Notifier> notifiers;
    
    public NotificationService(List<Notifier> notifiers) {
        this.notifiers = notifiers;
        log.info("Initialized NotificationService with {} notifiers", notifiers.size());
    }
    
    @Override
    public void notifySuccess(BackupResult result) {
        notifiers.forEach(notifier -> {
            try {
                notifier.notifySuccess(result);
            } catch (Exception e) {
                log.error("Notifier {} failed", notifier.getClass().getSimpleName(), e);
            }
        });
    }
    
    @Override
    public void notifyFailure(String message, Throwable error) {
        notifiers.forEach(notifier -> {
            try {
                notifier.notifyFailure(message, error);
            } catch (Exception e) {
                log.error("Notifier {} failed", notifier.getClass().getSimpleName(), e);
            }
        });
    }
}
