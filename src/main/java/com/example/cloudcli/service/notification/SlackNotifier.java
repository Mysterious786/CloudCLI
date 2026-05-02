package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.BackupResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Slack notifier using Webhook API
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "backup.notification.slack", name = "enabled", havingValue = "true")
public class SlackNotifier implements Notifier {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${backup.notification.slack.webhook-url}")
    private String webhookUrl;
    
    @Value("${backup.notification.slack.channel:#backups}")
    private String channel;
    
    @Value("${backup.notification.slack.username:Backup Bot}")
    private String username;
    
    public SlackNotifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public void notifySuccess(BackupResult result) {
        String message = String.format(
            "✅ *Backup Successful*\n" +
            "• Database: `%s`\n" +
            "• Type: `%s`\n" +
            "• Size: `%.2f MB`\n" +
            "• Duration: `%d seconds`\n" +
            "• Location: `%s`",
            result.getDatabaseName(),
            result.getBackupType(),
            result.getSizeInBytes() / (1024.0 * 1024.0),
            result.getDurationSeconds(),
            result.getStorageLocation()
        );
        
        sendSlackMessage(message, "good");
    }
    
    @Override
    public void notifyFailure(String message, Throwable error) {
        String slackMessage = String.format(
            "❌ *Backup Failed*\n" +
            "• Message: `%s`\n" +
            "• Error: `%s`",
            message,
            error != null ? error.getMessage() : "Unknown error"
        );
        
        sendSlackMessage(slackMessage, "danger");
    }
    
    private void sendSlackMessage(String text, String color) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channel);
            payload.put("username", username);
            payload.put("text", text);
            payload.put("icon_emoji", ":floppy_disk:");
            
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", color);
            attachment.put("text", text);
            payload.put("attachments", new Object[]{attachment});
            
            webClient.post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.info("Slack notification sent to {}", channel);
            
        } catch (Exception e) {
            log.error("Failed to send Slack notification", e);
        }
    }
}
