package com.example.cloudcli.cli;

import com.example.cloudcli.model.DatabaseConfig;
import com.example.cloudcli.service.SessionManager;
import com.example.cloudcli.service.ai.AiDocumentationService;
import com.example.cloudcli.service.ai.DocumentExporter;
import com.example.cloudcli.service.ai.SchemaExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

/**
 * AI Documentation Generator Command
 * Usage: cloudcli doc --type=postgres --host=localhost --database=mydb --username=postgres
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "doc",
    aliases = {"docs", "document"},
    description = "Generate AI-powered database documentation",
    mixinStandardHelpOptions = true
)
public class DocCommand implements Callable<Integer> {

    private final AiDocumentationService aiService;
    private final SchemaExtractorService schemaExtractor;
    private final DocumentExporter exporter;
    private final SessionManager sessionManager;

    @Option(names = {"-t", "--type"},
        description = "Database type: mysql, postgres, mongodb, sqlite, supabase",
        required = true)
    private String dbType;

    @Option(names = {"-d", "--database"},
        description = "Database name or SQLite file path",
        required = true)
    private String database;

    @Option(names = {"-h", "--host"},
        description = "Database host (default: localhost)",
        defaultValue = "localhost")
    private String host;

    @Option(names = {"-p", "--port"},
        description = "Database port",
        defaultValue = "0")
    private int port;

    @Option(names = {"-u", "--username"},
        description = "Database username")
    private String username;

    @Option(names = {"-P", "--password"},
        description = "Database password",
        interactive = true,
        arity = "0..1")
    private String password;

    @Option(names = {"-f", "--format"},
        description = "Output format: markdown (default), txt",
        defaultValue = "markdown")
    private String format;

    @Option(names = {"-o", "--output"},
        description = "Output directory (default: ./docs)",
        defaultValue = "./docs")
    private String outputDir;

    @Override
    public Integer call() {
        // ── Auth check ────────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            System.err.println("❌ You must be logged in to use this feature.");
            System.err.println("   Run: cloudcli login -u <username>");
            return 1;
        }

        // ── AI check ─────────────────────────────────────────────────────────
        if (!aiService.isConfigured()) {
            System.err.println("❌ OpenAI API key not configured.");
            System.err.println("   Add to ~/.cloudcli/.env:");
            System.err.println("   OPENAI_API_KEY=your-key-here");
            return 1;
        }

        // ── Format validation ─────────────────────────────────────────────────
        if (!format.equalsIgnoreCase("markdown") && !format.equalsIgnoreCase("md")
                && !format.equalsIgnoreCase("txt") && !format.equalsIgnoreCase("text")) {
            System.err.println("❌ Invalid format: " + format);
            System.err.println("   Supported formats: markdown, txt");
            return 1;
        }

        // ── Build config ──────────────────────────────────────────────────────
        DatabaseConfig config = new DatabaseConfig();
        config.setType(dbType.toLowerCase());
        config.setDatabase(database);
        config.setHost(host);
        config.setPort(port > 0 ? port : getDefaultPort(dbType));
        config.setUsername(username);
        config.setPassword(password);

        System.out.println("");
        System.out.println("  ============================================================");
        System.out.println("  CloudCLI AI Documentation Generator");
        System.out.println("  ============================================================");
        System.out.println("");
        System.out.println("  Database : " + dbType.toUpperCase() + " - " + database);
        System.out.println("  Format   : " + format.toUpperCase());
        System.out.println("  Output   : " + outputDir);
        System.out.println("");

        // ── Step 1: Extract schema ────────────────────────────────────────────
        System.out.print("  [1/3] Extracting database schema...");
        String schema;
        try {
            schema = schemaExtractor.extractSchema(config);
            System.out.println(" done! (" + countLines(schema) + " lines)");
        } catch (Exception e) {
            System.out.println(" failed!");
            System.err.println("  ❌ Schema extraction failed: " + e.getMessage());
            System.err.println("");
            System.err.println("  Troubleshooting:");
            printTroubleshootingTips(dbType, e.getMessage());
            return 1;
        }

        // ── Step 2: Generate AI documentation ────────────────────────────────
        System.out.print("  [2/3] Generating AI documentation (this may take 10-30 seconds)...");
        String documentation;
        try {
            documentation = aiService.generateDocumentation(schema, config, format);
            System.out.println(" done!");
        } catch (Exception e) {
            System.out.println(" failed!");
            System.err.println("  ❌ AI generation failed: " + e.getMessage());
            return 1;
        }

        // ── Step 3: Export to file ────────────────────────────────────────────
        System.out.print("  [3/3] Exporting documentation...");
        String outputPath;
        try {
            outputPath = exporter.export(documentation, database, format, outputDir);
            System.out.println(" done!");
        } catch (Exception e) {
            System.out.println(" failed!");
            System.err.println("  ❌ Export failed: " + e.getMessage());
            return 1;
        }

        // ── Success ───────────────────────────────────────────────────────────
        System.out.println("");
        System.out.println("  ============================================================");
        System.out.println("  ✅ Documentation generated successfully!");
        System.out.println("  ============================================================");
        System.out.println("");
        System.out.println("  File: " + outputPath);
        System.out.println("");
        System.out.println("  Preview (first 500 chars):");
        System.out.println("  ------------------------------------------------------------");
        String preview = documentation.length() > 500
            ? documentation.substring(0, 500) + "..."
            : documentation;
        for (String line : preview.split("\n")) {
            System.out.println("  " + line);
        }
        System.out.println("  ------------------------------------------------------------");
        System.out.println("");

        log.info("Documentation generated for {} database: {}", dbType, outputPath);
        return 0;
    }

    private int getDefaultPort(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql"              -> 3306;
            case "postgres","supabase"-> 5432;
            case "mongodb"            -> 27017;
            default                   -> 0;
        };
    }

    private int countLines(String text) {
        return text.split("\n").length;
    }

    private void printTroubleshootingTips(String dbType, String error) {
        switch (dbType.toLowerCase()) {
            case "postgres", "supabase" -> {
                System.err.println("  • Ensure pg_dump is installed and in PATH");
                System.err.println("  • Check host, port, username and password");
                System.err.println("  • For Supabase: use the connection pooler host");
                if (error != null && error.contains("password")) {
                    System.err.println("  • Try passing password with -P flag");
                }
            }
            case "mysql" -> {
                System.err.println("  • Ensure mysqldump is installed and in PATH");
                System.err.println("  • Check MySQL credentials and host");
            }
            case "sqlite" -> {
                System.err.println("  • Ensure the SQLite file path is correct");
                System.err.println("  • Check file permissions");
            }
            case "mongodb" -> {
                System.err.println("  • Ensure MongoDB is running");
                System.err.println("  • Check connection string and credentials");
            }
        }
    }
}
