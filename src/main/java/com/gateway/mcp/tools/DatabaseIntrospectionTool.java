package com.gateway.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * Enterprise Database Introspection Tool for AI Agents.
 *
 * Reads schema metadata (tables, column names, SQL data types, primary keys)
 * directly from JDBC Connection metadata without executing arbitrary SQL.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIntrospectionTool implements McpTool {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.singletonMap(
                "table_pattern", Map.of(
                        "type", "string",
                        "description", "Optional search pattern for table names (e.g. 'EMP%', or '%' for all tables)"
                )
        ));
        schema.put("required", Collections.emptyList());

        return McpToolDefinition.builder()
                .name("db_introspect_schema")
                .description("Safely introspects database schema, listing tables, column names, SQL types, and primary keys.")
                .inputSchema(schema)
                .build();
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        String pattern = "%";
        if (arguments != null && arguments.containsKey("table_pattern") && arguments.get("table_pattern") != null) {
            pattern = arguments.get("table_pattern").toString().trim();
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<Map<String, Object>> tablesList = new ArrayList<>();

            try (ResultSet rs = meta.getTables(null, null, pattern, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    Map<String, Object> tableInfo = new LinkedHashMap<>();
                    tableInfo.put("table_name", tableName);

                    // Fetch Columns
                    List<Map<String, String>> columns = new ArrayList<>();
                    try (ResultSet colRs = meta.getColumns(null, null, tableName, "%")) {
                        while (colRs.next()) {
                            Map<String, String> col = new LinkedHashMap<>();
                            col.put("name", colRs.getString("COLUMN_NAME"));
                            col.put("type", colRs.getString("TYPE_NAME"));
                            col.put("nullable", colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO");
                            columns.add(col);
                        }
                    }
                    tableInfo.put("columns", columns);

                    // Fetch Primary Keys
                    List<String> primaryKeys = new ArrayList<>();
                    try (ResultSet pkRs = meta.getPrimaryKeys(null, null, tableName)) {
                        while (pkRs.next()) {
                            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
                        }
                    }
                    tableInfo.put("primary_keys", primaryKeys);

                    tablesList.add(tableInfo);
                }
            }

            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tablesList);
            return McpToolResult.text(jsonOutput);

        } catch (Exception ex) {
            log.error("Failed to introspect schema: {}", ex.getMessage(), ex);
            return McpToolResult.error("Error inspecting database schema: " + ex.getMessage());
        }
    }
}
