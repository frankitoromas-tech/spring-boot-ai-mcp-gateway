package com.gateway.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;
import com.gateway.mcp.security.SqlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

/**
 * Enterprise Safe Query Executor Tool for AI Agents.
 *
 * Runs strictly sanitized read-only SQL queries with hard limits on
 * execution time and returned rows.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeQueryExecutorTool implements McpTool {

    private final DataSource dataSource;
    private final SqlSanitizer sqlSanitizer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mcp.security.max-rows-returned:100}")
    private int maxRowsReturned;

    @Value("${mcp.security.query-timeout-seconds:5}")
    private int queryTimeoutSeconds;

    @Override
    public McpToolDefinition getDefinition() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "query", Map.of(
                        "type", "string",
                        "description", "Strictly read-only SQL query (SELECT, WITH) to execute against database"
                )
        ));
        schema.put("required", Collections.singletonList("query"));

        return McpToolDefinition.builder()
                .name("db_execute_safe_query")
                .description("Executes a read-only SQL query safely. Blocks mutations, injection, and multi-statements.")
                .inputSchema(schema)
                .build();
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        if (arguments == null || !arguments.containsKey("query")) {
            return McpToolResult.error("Missing required parameter: 'query'");
        }

        String rawQuery = arguments.get("query").toString();

        try {
            // 1. Sanitize & Enforce LIMIT
            String safeQuery = sqlSanitizer.validateAndEnforceLimit(rawQuery, maxRowsReturned);
            log.info("Executing sanitized AI query: {}", safeQuery);

            List<Map<String, Object>> rows = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.setQueryTimeout(queryTimeoutSeconds);

                try (ResultSet rs = stmt.executeQuery(safeQuery)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int columnCount = md.getColumnCount();

                    while (rs.next() && rows.size() < maxRowsReturned) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(md.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            }

            Map<String, Object> resultPayload = new LinkedHashMap<>();
            resultPayload.put("executed_query", safeQuery);
            resultPayload.put("rows_returned", rows.size());
            resultPayload.put("rows", rows);

            return McpToolResult.text(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultPayload));

        } catch (SecurityException secEx) {
            log.warn("Blocked unsafe AI query: {}", secEx.getMessage());
            return McpToolResult.error("Security Violation: " + secEx.getMessage());
        } catch (Exception ex) {
            log.error("Error executing query: {}", ex.getMessage(), ex);
            return McpToolResult.error("Database execution error: " + ex.getMessage());
        }
    }
}
