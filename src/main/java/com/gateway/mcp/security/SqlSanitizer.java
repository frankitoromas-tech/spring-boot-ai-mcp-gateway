package com.gateway.mcp.security;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enterprise SQL Sanitizer and Guard for AI-driven database queries.
 *
 * Ensures:
 * 1. Query is strictly read-only (SELECT / WITH).
 * 2. Multi-statement chaining (stacked queries via semicolons) is blocked.
 * 3. DDL/destructive DML keywords are rejected.
 * 4. Maximum row limit is enforced.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Component
public class SqlSanitizer {

    private static final Pattern FORBIDDEN_KEYWORDS = Pattern.compile(
            "\\b(DROP|ALTER|TRUNCATE|DELETE|UPDATE|INSERT|MERGE|EXEC|EXECUTE|CALL|CREATE|RENAME|GRANT|REVOKE|SHUTDOWN)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MULTIPLE_STATEMENTS = Pattern.compile(
            ";\\s*\\S+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ALLOWED_START = Pattern.compile(
            "^\\s*(SELECT|WITH|EXPLAIN)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validates and returns sanitized SQL query string.
     *
     * @param rawQuery The query requested by the AI model.
     * @param maxRows Maximum permitted rows.
     * @return Validated query with guaranteed LIMIT.
     * @throws SecurityException If the query violates read-only or injection constraints.
     */
    public String validateAndEnforceLimit(String rawQuery, int maxRows) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query parameter cannot be null or blank.");
        }

        String trimmed = rawQuery.trim();

        // 1. Must start with SELECT, WITH, or EXPLAIN
        if (!ALLOWED_START.matcher(trimmed).find()) {
            throw new SecurityException("Security Violation: Only read-only queries (SELECT, WITH, EXPLAIN) are permitted.");
        }

        // 2. Multi-statement query blocking
        if (MULTIPLE_STATEMENTS.matcher(trimmed).find()) {
            throw new SecurityException("Security Violation: Multi-statement execution (semicolon chaining) is forbidden.");
        }

        // 3. Keyword blacklist
        if (FORBIDDEN_KEYWORDS.matcher(trimmed).find()) {
            throw new SecurityException("Security Violation: Destructive DDL/DML keywords detected in query.");
        }

        // 4. Clean trailing semicolon if present
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        // 5. Append or enforce LIMIT
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!upper.contains("LIMIT")) {
            trimmed = trimmed + " LIMIT " + maxRows;
        }

        return trimmed;
    }
}
