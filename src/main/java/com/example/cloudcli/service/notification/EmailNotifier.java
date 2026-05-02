package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.BackupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Enhanced Email notifier with HTML templates and rich formatting
 * Supports multiple recipients, HTML emails, and beautiful templates
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "backup.notification.email", name = "enabled", havingValue = "true")
public class EmailNotifier implements Notifier {
    
    @Value("${backup.notification.email.smtp-host}")
    private String smtpHost;
    
    @Value("${backup.notification.email.smtp-port}")
    private int smtpPort;
    
    @Value("${backup.notification.email.username}")
    private String username;
    
    @Value("${backup.notification.email.password}")
    private String password;
    
    @Value("${backup.notification.email.from}")
    private String from;
    
    @Value("${backup.notification.email.to}")
    private String to;
    
    @Value("${backup.notification.email.use-tls:true}")
    private boolean useTls;
    
    @Value("${backup.notification.email.html-enabled:true}")
    private boolean htmlEnabled;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    
    @Override
    public void notifySuccess(BackupResult result) {
        String subject = "✅ Backup Successful: " + result.getDatabaseName();
        
        if (htmlEnabled) {
            String htmlBody = buildSuccessHtmlEmail(result);
            sendHtmlEmail(subject, htmlBody);
        } else {
            String plainBody = buildSuccessPlainEmail(result);
            sendPlainEmail(subject, plainBody);
        }
    }
    
    @Override
    public void notifyFailure(String message, Throwable error) {
        String subject = "🚨 Backup Failed - Action Required";
        
        if (htmlEnabled) {
            String htmlBody = buildFailureHtmlEmail(message, error);
            sendHtmlEmail(subject, htmlBody);
        } else {
            String plainBody = buildFailurePlainEmail(message, error);
            sendPlainEmail(subject, plainBody);
        }
    }
    
    /**
     * Build beautiful HTML email for successful backup
     */
    private String buildSuccessHtmlEmail(BackupResult result) {
        double sizeMB = result.getSizeInBytes() / (1024.0 * 1024.0);
        String formattedDate = DATE_FORMATTER.format(
            result.getTimestamp().atZone(java.time.ZoneId.systemDefault())
        );
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; font-weight: 600; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .status-badge { display: inline-block; background-color: #10b981; color: white; padding: 8px 16px; border-radius: 20px; font-weight: 600; margin-top: 15px; }
                    .content { padding: 30px; }
                    .info-grid { display: table; width: 100%%; margin: 20px 0; }
                    .info-row { display: table-row; }
                    .info-label { display: table-cell; padding: 12px 0; font-weight: 600; color: #6b7280; width: 40%%; }
                    .info-value { display: table-cell; padding: 12px 0; color: #1f2937; }
                    .stats-box { background-color: #f9fafb; border-left: 4px solid #10b981; padding: 15px; margin: 20px 0; border-radius: 5px; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; color: #6b7280; font-size: 12px; }
                    .button { display: inline-block; background-color: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 5px; font-weight: 600; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">✅</div>
                        <h1>Backup Completed Successfully</h1>
                        <p>CloudCLI Backup Manager</p>
                        <div class="status-badge">SUCCESS</div>
                    </div>
                    
                    <div class="content">
                        <h2 style="color: #1f2937; margin-top: 0;">Backup Details</h2>
                        
                        <div class="info-grid">
                            <div class="info-row">
                                <div class="info-label">📊 Database:</div>
                                <div class="info-value"><strong>%s</strong></div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">🔄 Backup Type:</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">💾 File Size:</div>
                                <div class="info-value">%.2f MB</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">⏱️ Duration:</div>
                                <div class="info-value">%d seconds</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">☁️ Storage:</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">📅 Timestamp:</div>
                                <div class="info-value">%s</div>
                            </div>
                        </div>
                        
                        <div class="stats-box">
                            <strong>✨ Backup Summary</strong><br>
                            Your database has been successfully backed up and securely stored in the cloud. 
                            The backup is ready for download or restoration at any time.
                        </div>
                        
                        <div style="text-align: center; margin-top: 30px;">
                            <a href="#" class="button">📥 Download Backup</a>
                            <a href="#" class="button">📊 View Dashboard</a>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p><strong>CloudCLI Backup Manager</strong></p>
                        <p>Automated backup notification • Your data is safe and secure</p>
                        <p style="margin-top: 10px; color: #9ca3af;">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
            result.getDatabaseName(),
            result.getBackupType(),
            sizeMB,
            result.getDurationSeconds(),
            result.getStorageLocation(),
            formattedDate
        );
    }
    
    /**
     * Build beautiful HTML email for failed backup
     */
    private String buildFailureHtmlEmail(String message, Throwable error) {
        String errorDetails = error != null ? error.getMessage() : "Unknown error";
        String stackTrace = error != null ? getStackTracePreview(error) : "No stack trace available";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; font-weight: 600; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .status-badge { display: inline-block; background-color: #991b1b; color: white; padding: 8px 16px; border-radius: 20px; font-weight: 600; margin-top: 15px; }
                    .content { padding: 30px; }
                    .error-box { background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 15px; margin: 20px 0; border-radius: 5px; }
                    .error-details { background-color: #f9fafb; padding: 15px; border-radius: 5px; font-family: monospace; font-size: 12px; overflow-x: auto; margin: 15px 0; }
                    .action-list { background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 5px; }
                    .action-list ol { margin: 10px 0; padding-left: 20px; }
                    .action-list li { margin: 8px 0; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; color: #6b7280; font-size: 12px; }
                    .button { display: inline-block; background-color: #ef4444; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 5px; font-weight: 600; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">🚨</div>
                        <h1>Backup Failed - Action Required</h1>
                        <p>CloudCLI Backup Manager</p>
                        <div class="status-badge">FAILED</div>
                    </div>
                    
                    <div class="content">
                        <h2 style="color: #1f2937; margin-top: 0;">Error Details</h2>
                        
                        <div class="error-box">
                            <strong>❌ Error Message:</strong><br>
                            %s
                        </div>
                        
                        <div class="error-details">
                            <strong>Technical Details:</strong><br>
                            %s
                        </div>
                        
                        <div class="action-list">
                            <strong>⚠️ Recommended Actions:</strong>
                            <ol>
                                <li>Check if the database server is running and accessible</li>
                                <li>Verify network connectivity and firewall rules</li>
                                <li>Confirm database credentials are correct</li>
                                <li>Review CloudCLI logs for more details</li>
                                <li>Try running the backup manually to reproduce the issue</li>
                            </ol>
                        </div>
                        
                        <div style="background-color: #f0f9ff; border-left: 4px solid #3b82f6; padding: 15px; margin: 20px 0; border-radius: 5px;">
                            <strong>💡 Need Help?</strong><br>
                            If this issue persists, check the CloudCLI documentation or contact your system administrator.
                        </div>
                        
                        <div style="text-align: center; margin-top: 30px;">
                            <a href="#" class="button">🔄 Retry Backup</a>
                            <a href="#" class="button">📋 View Logs</a>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p><strong>CloudCLI Backup Manager</strong></p>
                        <p>Automated backup notification • Immediate attention required</p>
                        <p style="margin-top: 10px; color: #9ca3af;">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
            message,
            errorDetails,
            stackTrace
        );
    }
    
    /**
     * Build plain text email for successful backup (fallback)
     */
    private String buildSuccessPlainEmail(BackupResult result) {
        double sizeMB = result.getSizeInBytes() / (1024.0 * 1024.0);
        String formattedDate = DATE_FORMATTER.format(
            result.getTimestamp().atZone(java.time.ZoneId.systemDefault())
        );
        
        return String.format("""
            ✅ BACKUP SUCCESSFUL
            ═══════════════════════════════════════
            
            Your database backup has been completed successfully!
            
            BACKUP DETAILS:
            ───────────────────────────────────────
            Database:     %s
            Type:         %s
            Size:         %.2f MB
            Duration:     %d seconds
            Storage:      %s
            Timestamp:    %s
            
            ═══════════════════════════════════════
            CloudCLI Backup Manager
            Your data is safe and secure
            """,
            result.getDatabaseName(),
            result.getBackupType(),
            sizeMB,
            result.getDurationSeconds(),
            result.getStorageLocation(),
            formattedDate
        );
    }
    
    /**
     * Build plain text email for failed backup (fallback)
     */
    private String buildFailurePlainEmail(String message, Throwable error) {
        String errorDetails = error != null ? error.getMessage() : "Unknown error";
        
        return String.format("""
            🚨 BACKUP FAILED - ACTION REQUIRED
            ═══════════════════════════════════════
            
            Your backup operation has failed!
            
            ERROR DETAILS:
            ───────────────────────────────────────
            Message: %s
            Error:   %s
            
            RECOMMENDED ACTIONS:
            ───────────────────────────────────────
            1. Check if the database server is running
            2. Verify network connectivity
            3. Confirm database credentials
            4. Review CloudCLI logs
            5. Try running backup manually
            
            ═══════════════════════════════════════
            CloudCLI Backup Manager
            Immediate attention required
            """,
            message,
            errorDetails
        );
    }
    
    /**
     * Get first few lines of stack trace for email
     */
    private String getStackTracePreview(Throwable error) {
        if (error == null) return "No stack trace available";
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.getClass().getName()).append(": ").append(error.getMessage()).append("\n");
        
        StackTraceElement[] elements = error.getStackTrace();
        int limit = Math.min(5, elements.length);
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(elements[i].toString()).append("\n");
        }
        
        if (elements.length > limit) {
            sb.append("  ... ").append(elements.length - limit).append(" more");
        }
        
        return sb.toString();
    }
    
    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(useTls));
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            
            // Support multiple recipients (comma-separated)
            String[] recipients = to.split(",");
            InternetAddress[] addresses = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                addresses[i] = new InternetAddress(recipients[i].trim());
            }
            message.setRecipients(Message.RecipientType.TO, addresses);
            
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            
            Transport.send(message);
            log.info("HTML email notification sent to {} recipient(s)", recipients.length);
            
        } catch (MessagingException e) {
            log.error("Failed to send HTML email notification", e);
        }
    }
    
    /**
     * Send plain text email (fallback)
     */
    private void sendPlainEmail(String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(useTls));
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            
            // Support multiple recipients
            String[] recipients = to.split(",");
            InternetAddress[] addresses = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                addresses[i] = new InternetAddress(recipients[i].trim());
            }
            message.setRecipients(Message.RecipientType.TO, addresses);
            
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            log.info("Plain text email notification sent to {} recipient(s)", recipients.length);
            
        } catch (MessagingException e) {
            log.error("Failed to send plain text email notification", e);
        }
    }
}
