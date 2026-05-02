package com.example.cloudcli.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Document Exporter
 * Exports AI-generated documentation to Markdown or plain text
 * PDF export requires iText library
 */
@Slf4j
@Service
public class DocumentExporter {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export documentation to file
     */
    public String export(String content, String dbName, String format, String outputDir) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Documentation content cannot be empty");
        }

        // Create output directory
        Path dir = Paths.get(outputDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + outputDir, e);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = dbName.replaceAll("[^a-zA-Z0-9_-]", "_");

        return switch (format.toLowerCase()) {
            case "markdown", "md" -> exportMarkdown(content, safeName, timestamp, dir);
            case "txt", "text"    -> exportText(content, safeName, timestamp, dir);
            default -> throw new IllegalArgumentException(
                "Unsupported format: " + format + ". Use: markdown, txt"
            );
        };
    }

    // ── Markdown Export ───────────────────────────────────────────────────────
    private String exportMarkdown(String content, String dbName, String timestamp, Path dir) {
        String fileName = dbName + "_documentation_" + timestamp + ".md";
        Path filePath = dir.resolve(fileName);

        String header = String.format("""
            ---
            title: Database Documentation - %s
            generated: %s
            tool: CloudCLI AI Documentation Generator
            ---

            """, dbName, LocalDateTime.now().format(DATE_FMT));

        String fullContent = header + content;

        writeFile(filePath, fullContent);
        log.info("Markdown documentation exported to: {}", filePath);
        return filePath.toAbsolutePath().toString();
    }

    // ── Plain Text Export ─────────────────────────────────────────────────────
    private String exportText(String content, String dbName, String timestamp, Path dir) {
        String fileName = dbName + "_documentation_" + timestamp + ".txt";
        Path filePath = dir.resolve(fileName);

        // Convert markdown to plain text
        String plainText = content
            .replaceAll("#{1,6}\\s+", "")           // Remove headers
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // Remove bold
            .replaceAll("\\*(.+?)\\*", "$1")         // Remove italic
            .replaceAll("`(.+?)`", "$1")             // Remove inline code
            .replaceAll("```[\\s\\S]*?```", "")      // Remove code blocks
            .replaceAll("\\|", " | ")                // Format tables
            .replaceAll("-{3,}", "─".repeat(60));    // Format dividers

        String header = String.format(
            "DATABASE DOCUMENTATION - %s\nGenerated: %s\nTool: CloudCLI AI Documentation Generator\n%s\n\n",
            dbName.toUpperCase(),
            LocalDateTime.now().format(DATE_FMT),
            "═".repeat(60)
        );

        writeFile(filePath, header + plainText);
        log.info("Text documentation exported to: {}", filePath);
        return filePath.toAbsolutePath().toString();
    }

    // ── File Writer ───────────────────────────────────────────────────────────
    private void writeFile(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + path + " - " + e.getMessage(), e);
        }
    }
}
