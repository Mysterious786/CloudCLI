package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.BackupResult;

/**
 * Observer Pattern - Notifier interface for backup notifications
 */
public interface Notifier {
    void notifySuccess(BackupResult result);
    void notifyFailure(String message, Throwable error);
}
