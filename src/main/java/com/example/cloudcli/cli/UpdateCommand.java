package com.example.cloudcli.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.Callable;

/**
 * Self-update command - updates CloudCLI to the latest version
 * Usage: cloudcli update
 */
@Slf4j
@Component
@Command(
    name = "update",
    description = "Update CloudCLI to the latest version",
    mixinStandardHelpOptions = true
)
public class UpdateCommand implements Callable<Integer> {

    private static final String REPO        = "Mysterious786/CloudCLI";
    private static final String API_URL     = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String JAR_PATH    = System.getProperty("user.home") + "/.cloudcli/cloudcli.jar";
    private static final String CURRENT_VER = "1.0.5";

    @Override
    public Integer call() {
        System.out.println("");
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              ☁️  CloudCLI Updater                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("");
        System.out.println("  Current version : v" + CURRENT_VER);
        System.out.print("  Checking for updates...");

        try {
            // ── Get latest release info ───────────────────────────────────────
            String releaseInfo = fetchUrl(API_URL);
            String latestVersion = extractJson(releaseInfo, "tag_name");
            String downloadUrl   = extractDownloadUrl(releaseInfo);

            System.out.println(" done!");
            System.out.println("  Latest version  : " + latestVersion);
            System.out.println("");

            // ── Compare versions ──────────────────────────────────────────────
            if (("v" + CURRENT_VER).equals(latestVersion)) {
                System.out.println("  ✅ You're already on the latest version!");
                System.out.println("");
                return 0;
            }

            // ── Download new version ──────────────────────────────────────────
            System.out.println("  📥 Downloading " + latestVersion + "...");

            Path jarPath = Paths.get(JAR_PATH);
            Path backupPath = Paths.get(JAR_PATH + ".backup");

            // Backup current JAR
            if (Files.exists(jarPath)) {
                Files.copy(jarPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Download new JAR to temp file first
            Path tempPath = Paths.get(JAR_PATH + ".tmp");
            downloadWithProgress(downloadUrl, tempPath.toString());

            // Verify downloaded file is valid (must be > 10MB)
            long downloadedSize = Files.size(tempPath);
            if (downloadedSize < 10 * 1024 * 1024) {
                Files.deleteIfExists(tempPath);
                // Restore backup
                if (Files.exists(backupPath)) {
                    Files.copy(backupPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("\n  ❌ Downloaded file is too small (" + downloadedSize + " bytes). Aborting.");
                return 1;
            }

            // Replace old JAR with new one
            Files.move(tempPath, jarPath, StandardCopyOption.REPLACE_EXISTING);

            // Remove backup
            Files.deleteIfExists(backupPath);

            System.out.println("");
            System.out.println("  ✅ Updated to " + latestVersion + " successfully!");
            System.out.println("");
            System.out.println("  Restart CloudCLI to use the new version:");
            System.out.println("    cloudcli");
            System.out.println("");

            return 0;

        } catch (Exception e) {
            System.out.println("");
            System.out.println("  ❌ Update failed: " + e.getMessage());
            System.out.println("");
            System.out.println("  Manual update:");
            System.out.println("    curl -fsSL https://raw.githubusercontent.com/" + REPO + "/main/install.sh | bash");
            System.out.println("");
            log.error("Update failed", e);
            return 1;
        }
    }

    private void downloadWithProgress(String urlStr, String destPath) throws IOException {
        // Follow redirects
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "cloudcli-updater");

        // Handle redirects manually
        int status = conn.getResponseCode();
        while (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM
            || status == 307 || status == 308) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "cloudcli-updater");
            status = conn.getResponseCode();
        }

        long total = conn.getContentLengthLong();
        long downloaded = 0;

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (total > 0) {
                    int pct = (int) ((downloaded * 100) / total);
                    int bars = pct / 5;
                    String bar = "█".repeat(bars) + "░".repeat(20 - bars);
                    System.out.printf("\r  [%s] %d%%  %.1f MB / %.1f MB",
                        bar, pct,
                        downloaded / (1024.0 * 1024),
                        total / (1024.0 * 1024));
                }
            }
        }
    }

    private String fetchUrl(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "cloudcli-updater");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "unknown";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractDownloadUrl(String json) {
        // Find cloudcli.jar download URL
        String search = "browser_download_url";
        int idx = json.indexOf(search);
        while (idx != -1) {
            int start = json.indexOf("\"", idx + search.length() + 2) + 1;
            int end = json.indexOf("\"", start);
            String url = json.substring(start, end);
            if (url.endsWith("cloudcli.jar")) return url;
            idx = json.indexOf(search, end);
        }
        // Fallback to latest
        return "https://github.com/" + REPO + "/releases/latest/download/cloudcli.jar";
    }
}
