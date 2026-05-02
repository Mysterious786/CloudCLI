package com.example.cloudcli.cli;

import com.example.cloudcli.repository.BackupRecord;
import com.example.cloudcli.repository.InMemoryBackupRepository;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.ai.AiReportService;
import com.example.cloudcli.service.ai.DocumentExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.*;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * AI Backup Report Command
 * Usage: cloudcli report
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "report",
    description = "Generate AI-powered backup analysis report",
    mixinStandardHelpOptions = true
)
public class ReportCommand implements Callable<Integer> {

    private final AiReportService reportService;
    private final DocumentExporter exporter;
    private final InMemoryBackupRepository backupRepository;
    private final SessionManager sessionManager;

    @Option(names = {"-f", "--format"},
        description = "Output format: markdown (default), txt",
        defaultValue = "markdown")
    private String format;

    @Option(names = {"-o", "--output"},
        description = "Output directory (default: ./reports)",
        defaultValue = "./reports")
    private String outputDir;

    @Option(names = {"--print"},
        description = "Print report to terminal instead of saving to file",
        defaultValue = "false")
    private boolean printOnly;

    @Override
    public Integer call() {
        // Auth check
        if (!sessionManager.isLoggedIn()) {
            System.err.println("❌ You must be logged in.");
            System.err.println("   Run: cloudcli login -u <username>");
            return 1;
        }

        // AI check
        if (!reportService.isConfigured()) {
            System.err.println("❌ OpenAI API key not configured.");
            System.err.println("   Add OPENAI_API_KEY to ~/.cloudcli/.env");
            return 1;
        }

        String username = sessionManager.getCurrentUsername();
        String userId   = sessionManager.getCurrentUserId();

        System.out.println("");
        System.out.println("  ============================================================");
        System.out.println("  CloudCLI AI Backup Report");
        System.out.println("  ============================================================");
        System.out.println("  User: " + username);
        System.out.println("");

        // Get backup history
        List<BackupRecord> backups = backupRepository.findByUserId(userId);

        if (backups.isEmpty()) {
            System.err.println("  ❌ No backups found for user: " + username);
            System.err.println("  Create some backups first, then run this command.");
            return 1;
        }

        System.out.println("  Found " + backups.size() + " backup(s) to analyze.");
        System.out.println("");
        System.out.print("  Generating AI report (this may take 10-20 seconds)...");

        // Generate report
        String report;
        try {
            report = reportService.generateReport(backups, username);
            System.out.println(" done!");
        } catch (Exception e) {
            System.out.println(" failed!");
            System.err.println("  ❌ Report generation failed: " + e.getMessage());
            return 1;
        }

        // Print or save
        if (printOnly) {
            System.out.println("");
            System.out.println("  ============================================================");
            System.out.println(report);
            System.out.println("  ============================================================");
        } else {
            System.out.print("  Saving report...");
            try {
                String outputPath = exporter.export(report, username + "_backup_report", format, outputDir);
                System.out.println(" done!");
                System.out.println("");
                System.out.println("  ============================================================");
                System.out.println("  ✅ Report saved successfully!");
                System.out.println("  ============================================================");
                System.out.println("");
                System.out.println("  File: " + outputPath);
                System.out.println("");

                // Print preview
                System.out.println("  Preview:");
                System.out.println("  ------------------------------------------------------------");
                String[] lines = report.split("\n");
                int previewLines = Math.min(20, lines.length);
                for (int i = 0; i < previewLines; i++) {
                    System.out.println("  " + lines[i]);
                }
                if (lines.length > previewLines) {
                    System.out.println("  ... (" + (lines.length - previewLines) + " more lines in file)");
                }
                System.out.println("  ------------------------------------------------------------");
                System.out.println("");

            } catch (Exception e) {
                System.out.println(" failed!");
                System.err.println("  ❌ Failed to save report: " + e.getMessage());
                return 1;
            }
        }

        log.info("AI backup report generated for user: {}", username);
        return 0;
    }
}
