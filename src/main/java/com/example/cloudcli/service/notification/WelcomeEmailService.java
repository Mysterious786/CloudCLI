package com.example.cloudcli.service.notification;

import com.example.cloudcli.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Welcome Email Service - Sends beautiful welcome emails to new users
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "backup.notification.email", name = "enabled", havingValue = "true")
public class WelcomeEmailService {

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

    @Value("${backup.notification.email.use-tls:true}")
    private boolean useTls;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private static final String ADMIN_NAME         = "Saqlain Zarjis Ansari";
    private static final String ADMIN_PHONE        = "+91 8442883695";
    private static final String ADMIN_EMAIL_ADDR   = "saqlainzarjisansari@gmail.com";
    private static final String ADMIN_LINKEDIN     = "https://www.linkedin.com/in/saqlain-zarjis-ansari-108b2621b/";
    private static final String ADMIN_LINKEDIN_LBL = "Connect on LinkedIn";

    public void sendWelcomeEmail(User user) {
        try {
            String subject = "🎉 Welcome to CloudCLI – Your Backup Journey Starts Now!";
            String htmlBody = buildWelcomeEmail(user);
            sendEmail(user.getEmail(), subject, htmlBody);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    private String buildWelcomeEmail(User user) {
        String joinDate = DATE_FORMATTER.format(
            user.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
        );

        return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n"
            + "<title>Welcome to CloudCLI</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;padding:0;background-color:#0f0c29;\">\n"

            // ── OUTER TABLE ──────────────────────────────────────────────────
            + "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:linear-gradient(135deg,#0f0c29,#302b63,#24243e);min-height:100vh;\">\n"
            + "<tr><td align=\"center\" style=\"padding:40px 20px;\">\n"

            // ── CARD ─────────────────────────────────────────────────────────
            + "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"max-width:600px;background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 25px 60px rgba(0,0,0,0.5);\">\n"

            // ── HERO HEADER ──────────────────────────────────────────────────
            + "<tr>\n"
            + "<td style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 50%,#f64f59 100%);padding:0;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "    <tr>\n"
            + "      <td align=\"center\" style=\"padding:50px 30px 20px;\">\n"
            // big cloud emoji as hero image
            + "        <div style=\"font-size:80px;line-height:1;margin-bottom:20px;\">☁️</div>\n"
            + "        <h1 style=\"margin:0 0 10px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:34px;font-weight:800;color:#ffffff;letter-spacing:-1px;\">Welcome to CloudCLI!</h1>\n"
            + "        <p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:17px;color:rgba(255,255,255,0.9);\">Your backup journey starts today 🚀</p>\n"
            + "      </td>\n"
            + "    </tr>\n"
            // wave divider using a thick border trick
            + "    <tr>\n"
            + "      <td style=\"height:30px;background:linear-gradient(135deg,#667eea 0%,#764ba2 50%,#f64f59 100%);border-radius:0 0 50% 50%;\">&nbsp;</td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "</td>\n"
            + "</tr>\n"

            // ── GREETING ─────────────────────────────────────────────────────
            + "<tr><td style=\"padding:40px 40px 20px;\">\n"
            + "  <p style=\"margin:0 0 16px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:18px;color:#1f2937;line-height:1.7;\">\n"
            + "    Hi <strong style=\"color:#667eea;\">" + user.getUsername() + "</strong>,\n"
            + "  </p>\n"
            + "  <p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:16px;color:#4b5563;line-height:1.8;\">\n"
            + "    We're absolutely thrilled to have you join the <strong>CloudCLI</strong> family! 🎉<br>\n"
            + "    Your account is live and ready. You can now protect your valuable databases with our powerful, easy-to-use backup solution.\n"
            + "  </p>\n"
            + "</td></tr>\n"

            // ── STATS BANNER ─────────────────────────────────────────────────
            + "<tr><td style=\"padding:20px 40px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:linear-gradient(135deg,#667eea,#764ba2);border-radius:16px;overflow:hidden;\">\n"
            + "    <tr>\n"
            + "      <td align=\"center\" style=\"padding:28px 10px;border-right:1px solid rgba(255,255,255,0.2);\">\n"
            + "        <span style=\"display:block;font-size:40px;font-weight:800;color:#ffffff;font-family:Arial,sans-serif;\">5</span>\n"
            + "        <span style=\"display:block;font-size:11px;text-transform:uppercase;letter-spacing:2px;color:rgba(255,255,255,0.8);margin-top:6px;font-family:Arial,sans-serif;\">Databases</span>\n"
            + "      </td>\n"
            + "      <td align=\"center\" style=\"padding:28px 10px;border-right:1px solid rgba(255,255,255,0.2);\">\n"
            + "        <span style=\"display:block;font-size:40px;font-weight:800;color:#ffffff;font-family:Arial,sans-serif;\">4</span>\n"
            + "        <span style=\"display:block;font-size:11px;text-transform:uppercase;letter-spacing:2px;color:rgba(255,255,255,0.8);margin-top:6px;font-family:Arial,sans-serif;\">Cloud Providers</span>\n"
            + "      </td>\n"
            + "      <td align=\"center\" style=\"padding:28px 10px;\">\n"
            + "        <span style=\"display:block;font-size:40px;font-weight:800;color:#ffffff;font-family:Arial,sans-serif;\">∞</span>\n"
            + "        <span style=\"display:block;font-size:11px;text-transform:uppercase;letter-spacing:2px;color:rgba(255,255,255,0.8);margin-top:6px;font-family:Arial,sans-serif;\">Backups</span>\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "</td></tr>\n"

            // ── FEATURES HEADING ─────────────────────────────────────────────
            + "<tr><td style=\"padding:30px 40px 10px;\">\n"
            + "  <h2 style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:22px;font-weight:700;color:#1f2937;text-align:center;\">✨ Everything You Get</h2>\n"
            + "</td></tr>\n"

            // ── FEATURE CARDS (2-column grid) ────────────────────────────────
            + "<tr><td style=\"padding:10px 40px 20px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "    <tr>\n"
            + featureCard("🗄️", "#667eea", "Multi-Database", "MySQL, PostgreSQL,<br>MongoDB, SQLite, Supabase")
            + featureCard("☁️", "#764ba2", "Cloud Storage", "Backblaze B2, AWS S3,<br>Wasabi &amp; more")
            + "    </tr>\n"
            + "    <tr><td colspan=\"2\" style=\"height:12px;\"></td></tr>\n"
            + "    <tr>\n"
            + featureCard("🔐", "#f64f59", "Secure &amp; Encrypted", "Compressed backups with<br>industry-standard security")
            + featureCard("📧", "#10b981", "Instant Alerts", "Email notifications on<br>every backup event")
            + "    </tr>\n"
            + "    <tr><td colspan=\"2\" style=\"height:12px;\"></td></tr>\n"
            + "    <tr>\n"
            + featureCard("⚡", "#f59e0b", "Lightning Fast", "Optimised execution with<br>automatic retry on failure")
            + featureCard("📥", "#3b82f6", "Download Anytime", "Retrieve any backup<br>from anywhere, anytime")
            + "    </tr>\n"
            + "  </table>\n"
            + "</td></tr>\n"

            // ── CTA BUTTON ───────────────────────────────────────────────────
            + "<tr><td align=\"center\" style=\"padding:30px 40px;\">\n"
            + "  <table cellpadding=\"0\" cellspacing=\"0\">\n"
            + "    <tr>\n"
            + "      <td align=\"center\" style=\"background:linear-gradient(135deg,#667eea,#764ba2);border-radius:50px;\">\n"
            + "        <a href=\"#\" style=\"display:inline-block;padding:18px 50px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:17px;font-weight:700;color:#ffffff;text-decoration:none;\">🚀 Create Your First Backup</a>\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "</td></tr>\n"

            // ── QUICK START ──────────────────────────────────────────────────
            + "<tr><td style=\"padding:0 40px 30px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0fdf4;border-left:5px solid #10b981;border-radius:10px;\">\n"
            + "    <tr><td style=\"padding:24px 24px 8px;\">\n"
            + "      <h3 style=\"margin:0 0 16px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:17px;font-weight:700;color:#065f46;\">💡 Quick Start Guide</h3>\n"
            + "    </td></tr>\n"
            + tipRow("1", "Launch CloudCLI", "Run <code style=\"background:#d1fae5;padding:2px 6px;border-radius:4px;\">./cloudcli</code> in your terminal")
            + tipRow("2", "Choose Your Mode", "Command Shell for speed · Interactive Wizard for guidance")
            + tipRow("3", "Test Connection", "Always verify your DB connection before the first backup")
            + tipRow("4", "Schedule Backups", "Set up daily/weekly automated backups for peace of mind")
            + tipRow("5", "Download Anytime", "Retrieve any backup from anywhere with one command")
            + "    <tr><td style=\"height:16px;\"></td></tr>\n"
            + "  </table>\n"
            + "</td></tr>\n"

            // ── ACCOUNT DETAILS ──────────────────────────────────────────────
            + "<tr><td style=\"padding:0 40px 30px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#eff6ff;border-radius:12px;overflow:hidden;\">\n"
            + "    <tr><td colspan=\"2\" style=\"background:linear-gradient(135deg,#667eea,#764ba2);padding:14px 24px;\">\n"
            + "      <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:16px;font-weight:700;color:#ffffff;\">📋 Your Account Details</span>\n"
            + "    </td></tr>\n"
            + accountRow("👤 Username", user.getUsername(), false)
            + accountRow("📧 Email", user.getEmail(), false)
            + accountRow("📅 Joined", joinDate, false)
            + accountRow("🔑 User ID", "<span style=\"font-family:monospace;font-size:12px;\">" + user.getUserId() + "</span>", true)
            + "  </table>\n"
            + "</td></tr>\n"

            // ── CONTACT / SUPPORT ────────────────────────────────────────────
            + "<tr><td style=\"padding:0 40px 40px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-radius:16px;overflow:hidden;\">\n"
            // header
            + "    <tr><td style=\"background:linear-gradient(135deg,#f64f59,#c471ed,#12c2e9);padding:24px;text-align:center;\">\n"
            + "      <span style=\"font-size:36px;\">💪</span><br>\n"
            + "      <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:20px;font-weight:700;color:#ffffff;\">Need Help? We're Here!</span><br>\n"
            + "      <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;color:rgba(255,255,255,0.9);\">Reach out directly — we respond fast</span>\n"
            + "    </td></tr>\n"
            // contact rows
            + "    <tr><td style=\"background:#1f2937;padding:24px;\">\n"
            + contactRow("👤", "Developer", ADMIN_NAME, null)
            + contactRow("📞", "Phone", ADMIN_PHONE, "tel:+918442883695")
            + contactRow("📧", "Email", ADMIN_EMAIL_ADDR, "mailto:" + ADMIN_EMAIL_ADDR)
            + contactRow("💼", "LinkedIn", ADMIN_LINKEDIN_LBL, ADMIN_LINKEDIN)
            + "    </td></tr>\n"
            + "  </table>\n"
            + "</td></tr>\n"

            // ── FOOTER ───────────────────────────────────────────────────────
            + "<tr><td style=\"background:#111827;padding:36px 40px;text-align:center;\">\n"
            + "  <div style=\"font-size:32px;margin-bottom:10px;\">☁️</div>\n"
            + "  <p style=\"margin:0 0 6px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:18px;font-weight:800;color:#667eea;\">CloudCLI</p>\n"
            + "  <p style=\"margin:0 0 6px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:13px;color:#9ca3af;\">Backup Manager · Protecting your data, one backup at a time</p>\n"
            + "  <p style=\"margin:0 0 20px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:13px;color:#6b7280;\">Developed with ❤️ by <strong style=\"color:#a78bfa;\">" + ADMIN_NAME + "</strong></p>\n"
            + "  <p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:11px;color:#4b5563;line-height:1.6;\">\n"
            + "    This is an automated welcome message from CloudCLI.<br>\n"
            + "    You're receiving this because you created an account with us.<br>\n"
            + "    © 2026 CloudCLI. All rights reserved.\n"
            + "  </p>\n"
            + "</td></tr>\n"

            + "</table>\n"  // end card
            + "</td></tr>\n"
            + "</table>\n"  // end outer
            + "</body>\n"
            + "</html>\n";
    }

    // ── Helper: feature card cell ─────────────────────────────────────────────
    private String featureCard(String icon, String color, String title, String desc) {
        return "<td width=\"50%\" style=\"padding:0 6px;\">\n"
            + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f9fafb;border-radius:12px;border-top:4px solid " + color + ";\">\n"
            + "    <tr><td style=\"padding:18px;\">\n"
            + "      <span style=\"font-size:30px;\">" + icon + "</span>\n"
            + "      <p style=\"margin:10px 0 6px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:700;color:#1f2937;\">" + title + "</p>\n"
            + "      <p style=\"margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:13px;color:#6b7280;line-height:1.5;\">" + desc + "</p>\n"
            + "    </td></tr>\n"
            + "  </table>\n"
            + "</td>\n";
    }

    // ── Helper: tip row ───────────────────────────────────────────────────────
    private String tipRow(String num, String title, String desc) {
        return "<tr><td style=\"padding:6px 24px;\">\n"
            + "  <table cellpadding=\"0\" cellspacing=\"0\">\n"
            + "    <tr>\n"
            + "      <td style=\"width:28px;height:28px;background:#10b981;border-radius:50%;text-align:center;vertical-align:middle;\">\n"
            + "        <span style=\"font-family:Arial,sans-serif;font-size:13px;font-weight:700;color:#ffffff;\">" + num + "</span>\n"
            + "      </td>\n"
            + "      <td style=\"padding-left:12px;\">\n"
            + "        <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:700;color:#065f46;\">" + title + ":</span>\n"
            + "        <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;color:#374151;\"> " + desc + "</span>\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "</td></tr>\n";
    }

    // ── Helper: account row ───────────────────────────────────────────────────
    private String accountRow(String label, String value, boolean last) {
        String border = last ? "" : "border-bottom:1px solid #dbeafe;";
        return "<tr>\n"
            + "  <td style=\"padding:12px 24px;" + border + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;font-weight:600;color:#6b7280;width:38%;\">" + label + "</td>\n"
            + "  <td style=\"padding:12px 24px;" + border + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:14px;color:#1f2937;\">" + value + "</td>\n"
            + "</tr>\n";
    }

    // ── Helper: contact row ───────────────────────────────────────────────────
    private String contactRow(String icon, String label, String value, String href) {
        String valueHtml = href != null
            ? "<a href=\"" + href + "\" style=\"color:#a78bfa;font-weight:600;text-decoration:none;\">" + value + "</a>"
            : "<span style=\"color:#e5e7eb;\">" + value + "</span>";
        return "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:14px;\">\n"
            + "  <tr>\n"
            + "    <td style=\"width:36px;height:36px;background:#374151;border-radius:8px;text-align:center;vertical-align:middle;font-size:18px;\">" + icon + "</td>\n"
            + "    <td style=\"padding-left:14px;\">\n"
            + "      <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:12px;text-transform:uppercase;letter-spacing:1px;color:#6b7280;display:block;\">" + label + "</span>\n"
            + "      <span style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;font-size:15px;\">" + valueHtml + "</span>\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "</table>\n";
    }

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
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
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
    }
}
