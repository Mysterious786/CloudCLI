package com.example.cloudcli.service.ai;

import com.example.cloudcli.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * AI Documentation Service using OpenAI GPT
 * Generates human-readable documentation from database schemas
 */
@Slf4j
@Service
public class AiDocumentationService {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    /**
     * Check if OpenAI is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("your-openai-key");
    }

    /**
     * Generate documentation from schema SQL
     */
    public String generateDocumentation(String schemaSQL, DatabaseConfig config, String format) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                "OpenAI API key not configured. Add OPENAI_API_KEY to ~/.cloudcli/.env"
            );
        }

        if (schemaSQL == null || schemaSQL.isBlank()) {
            throw new IllegalArgumentException("Schema SQL cannot be empty");
        }

        // Truncate schema if too large (OpenAI token limit)
        String truncatedSchema = schemaSQL.length() > 15000
            ? schemaSQL.substring(0, 15000) + "\n... (truncated)"
            : schemaSQL;

        String prompt = buildPrompt(truncatedSchema, config, format);
        return callOpenAI(prompt);
    }

    /**
     * Build the prompt for OpenAI
     */
    private String buildPrompt(String schema, DatabaseConfig config, String format) {
        String dbType = config.getType() != null ? config.getType().toUpperCase() : "SQL";
        String dbName = config.getDatabase() != null ? config.getDatabase() : "database";

        String formatInstructions = format.equalsIgnoreCase("markdown")
            ? "Format the output as clean Markdown with headers (##), tables, and code blocks."
            : "Format the output as plain text with clear sections and indentation.";

        return String.format("""
            You are a senior database architect. Analyze the following %s database schema for the database '%s' and generate comprehensive documentation.

            %s

            The documentation should include:

            1. **Executive Summary** - Brief overview of what this database is for based on the schema
            2. **Database Overview** - Total tables, views, indexes, relationships
            3. **Table Documentation** - For each table:
               - Purpose and description (infer from column names)
               - Column details (name, type, nullable, description)
               - Primary keys and foreign keys
               - Indexes
               - Estimated row count category (small/medium/large based on design)
            4. **Relationships** - Entity relationships between tables
            5. **Security Notes** - Identify sensitive columns (passwords, emails, tokens, PII)
            6. **Recommendations** - Missing indexes, potential improvements, best practices
            7. **Glossary** - Key terms and column name explanations

            Be specific and insightful. Infer the business purpose from column and table names.

            DATABASE SCHEMA:
            ```sql
            %s
            ```
            """,
            dbType, dbName, formatInstructions, schema
        );
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAI(String prompt) {
        // Escape the prompt for JSON
        String escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        String requestBody = String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a senior database architect who creates clear, comprehensive database documentation."
                    },
                    {
                        "role": "user",
                        "content": "%s"
                    }
                ],
                "max_tokens": 4000,
                "temperature": 0.3
            }
            """, model, escapedPrompt);

        Request request = new Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                handleApiError(response.code(), errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return extractContent(responseBody);

        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to OpenAI API: " + e.getMessage(), e);
        }
    }

    /**
     * Extract content from OpenAI response JSON
     */
    private String extractContent(String responseJson) {
        // Simple JSON parsing without external library
        String marker = "\"content\":\"";
        int start = responseJson.indexOf(marker);
        if (start == -1) {
            // Try alternate format
            marker = "\"content\": \"";
            start = responseJson.indexOf(marker);
        }
        if (start == -1) {
            log.error("Unexpected OpenAI response: {}", responseJson);
            throw new RuntimeException("Could not parse OpenAI response");
        }

        start += marker.length();
        StringBuilder content = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> content.append('\n');
                    case 't' -> content.append('\t');
                    case 'r' -> content.append('\r');
                    case '"' -> content.append('"');
                    case '\\' -> content.append('\\');
                    default -> content.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // End of content
            } else {
                content.append(c);
            }
        }

        return content.toString();
    }

    /**
     * Handle API errors with clear messages
     */
    private void handleApiError(int statusCode, String errorBody) {
        String message = switch (statusCode) {
            case 401 -> "Invalid OpenAI API key. Check your OPENAI_API_KEY in ~/.cloudcli/.env";
            case 429 -> "OpenAI rate limit exceeded. Please wait a moment and try again.";
            case 500, 502, 503 -> "OpenAI service is temporarily unavailable. Try again later.";
            case 400 -> "Bad request to OpenAI API. Schema may be too large.";
            default -> "OpenAI API error (HTTP " + statusCode + "): " + errorBody;
        };
        throw new RuntimeException(message);
    }
}
