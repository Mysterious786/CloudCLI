package com.example.cloudcli.service.ai;

import com.example.cloudcli.repository.BackupRecord;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI Backup Report Service
 * Analyzes backup history and generates intelligent reports using OpenAI
 */
@Slf4j
@Service
public class AiReportService {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generate AI-powered backup report from backup history
     */
    public String generateReport(List<BackupRecord> backups, String username) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI API key not configured.");
        }
        if (backups == null || backups.isEmpty()) {
            throw new IllegalArgumentException("No backup history found to analyze.");
        }

        String summary = buildBackupSummary(backups, username);
        String prompt  = buildReportPrompt(summary, username);
        return callOpenAI(prompt);
    }

    // ── Build structured summary from backup records ──────────────────────────
    private String buildBackupSummary(List<BackupRecord> backups, String username) {
        StringBuilder sb = new StringBuilder();

        // Overall stats
        int total = backups.size();
        long totalBytes = backups.stream().mapToLong(BackupRecord::getSizeBytes).sum();
        long avgDuration = (long) backups.stream()
            .mapToLong(BackupRecord::getDurationSeconds).average().orElse(0);

        // Group by database
        Map<String, List<BackupRecord>> byDb = backups.stream()
            .collect(Collectors.groupingBy(BackupRecord::getDatabaseName));

        // Group by type
        Map<String, Long> byType = backups.stream()
            .collect(Collectors.groupingBy(BackupRecord::getBackupType, Collectors.counting()));

        // Time range
        Instant oldest = backups.stream().map(BackupRecord::getTimestamp).min(Instant::compareTo).orElse(Instant.now());
        Instant newest = backups.stream().map(BackupRecord::getTimestamp).max(Instant::compareTo).orElse(Instant.now());

        // Size trend (last 5 vs first 5)
        String sizeTrend = "stable";
        if (backups.size() >= 6) {
            double firstAvg = backups.subList(0, 3).stream()
                .mapToLong(BackupRecord::getSizeBytes).average().orElse(0);
            double lastAvg = backups.subList(backups.size() - 3, backups.size()).stream()
                .mapToLong(BackupRecord::getSizeBytes).average().orElse(0);
            if (lastAvg > firstAvg * 1.2) sizeTrend = "growing";
            else if (lastAvg < firstAvg * 0.8) sizeTrend = "shrinking";
        }

        sb.append("USER: ").append(username).append("\n");
        sb.append("REPORT DATE: ").append(FMT.format(Instant.now())).append("\n\n");

        sb.append("=== OVERALL STATISTICS ===\n");
        sb.append("Total backups: ").append(total).append("\n");
        sb.append("Total data protected: ").append(formatSize(totalBytes)).append("\n");
        sb.append("Average backup duration: ").append(avgDuration).append(" seconds\n");
        sb.append("Date range: ").append(FMT.format(oldest)).append(" to ").append(FMT.format(newest)).append("\n");
        sb.append("Size trend: ").append(sizeTrend).append("\n\n");

        sb.append("=== BACKUP TYPES ===\n");
        byType.forEach((type, count) ->
            sb.append(type).append(": ").append(count).append(" backups\n"));
        sb.append("\n");

        sb.append("=== DATABASES ===\n");
        byDb.forEach((db, records) -> {
            long dbTotal = records.stream().mapToLong(BackupRecord::getSizeBytes).sum();
            Instant lastBackup = records.stream().map(BackupRecord::getTimestamp)
                .max(Instant::compareTo).orElse(Instant.now());
            long daysSinceLastBackup = (Instant.now().getEpochSecond() - lastBackup.getEpochSecond()) / 86400;

            sb.append("Database: ").append(db).append("\n");
            sb.append("  Backup count: ").append(records.size()).append("\n");
            sb.append("  Total size: ").append(formatSize(dbTotal)).append("\n");
            sb.append("  Last backup: ").append(FMT.format(lastBackup))
              .append(" (").append(daysSinceLastBackup).append(" days ago)\n");

            // Size trend per DB
            if (records.size() >= 2) {
                BackupRecord first = records.get(0);
                BackupRecord last = records.get(records.size() - 1);
                double growth = last.getSizeBytes() > 0
                    ? ((double)(last.getSizeBytes() - first.getSizeBytes()) / first.getSizeBytes()) * 100
                    : 0;
                sb.append("  Size change: ").append(String.format("%.1f%%", growth)).append("\n");
            }
            sb.append("\n");
        });

        sb.append("=== RECENT BACKUPS (last 5) ===\n");
        backups.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(5)
            .forEach(b -> sb.append(String.format("  %s | %s | %s | %s | %ds\n",
                FMT.format(b.getTimestamp()),
                b.getDatabaseName(),
                b.getBackupType(),
                formatSize(b.getSizeBytes()),
                b.getDurationSeconds())));

        return sb.toString();
    }

    // ── Build OpenAI prompt ───────────────────────────────────────────────────
    private String buildReportPrompt(String summary, String username) {
        return String.format("""
            You are a senior DevOps engineer analyzing backup data for user '%s'.
            Based on the following backup statistics, generate a comprehensive, actionable backup report.

            The report should include:
            1. **Executive Summary** - 2-3 sentences overview
            2. **Key Metrics** - Important numbers highlighted
            3. **Database Analysis** - Per-database insights
            4. **Risk Assessment** - Any databases at risk (not backed up recently, growing fast, etc.)
            5. **Trends** - Size trends, frequency patterns
            6. **Recommendations** - Specific, actionable steps to improve backup strategy
            7. **Health Score** - Overall backup health score out of 10 with explanation

            Be specific, use the actual data provided. Flag any risks clearly.
            Format as clean Markdown.

            BACKUP DATA:
            %s
            """, username, summary);
    }

    // ── Call OpenAI ───────────────────────────────────────────────────────────
    private String callOpenAI(String prompt) {
        String escaped = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        String body = String.format("""
            {
                "model": "%s",
                "messages": [
                    {"role": "system", "content": "You are a senior DevOps engineer who creates clear, actionable backup reports."},
                    {"role": "user", "content": "%s"}
                ],
                "max_tokens": 2000,
                "temperature": 0.4
            }
            """, model, escaped);

        Request request = new Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(body, JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String msg = switch (code) {
                    case 401 -> "Invalid OpenAI API key";
                    case 429 -> "Rate limit exceeded. Try again in a moment.";
                    default  -> "OpenAI API error: HTTP " + code;
                };
                throw new RuntimeException(msg);
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            return extractContent(responseBody);
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to OpenAI: " + e.getMessage(), e);
        }
    }

    private String extractContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) { marker = "\"content\": \""; start = json.indexOf(marker); }
        if (start == -1) throw new RuntimeException("Could not parse OpenAI response");

        start += marker.length();
        StringBuilder content = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> content.append('\n');
                    case 't' -> content.append('\t');
                    case '"' -> content.append('"');
                    case '\\' -> content.append('\\');
                    default  -> content.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
            }
        }
        return content.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
