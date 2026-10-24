package com.gateway.mcp;

import com.gateway.mcp.protocol.model.McpRequest;
import com.gateway.mcp.protocol.model.McpResponse;
import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;
import com.gateway.mcp.tools.McpToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Integration Tests for MCP Gateway and Tool Dispatches.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@SpringBootTest
class McpGatewayApplicationTests {

    @Autowired
    private McpToolRegistry toolRegistry;

    @Test
    @DisplayName("Context Loads & Tools are registered in McpToolRegistry")
    void contextLoadsAndRegistersTools() {
        List<McpToolDefinition> definitions = toolRegistry.listToolDefinitions();
        assertThat(definitions).isNotEmpty();
        assertThat(definitions).extracting(McpToolDefinition::getName)
                .contains("db_introspect_schema", "db_execute_safe_query", "system_get_telemetry");
    }

    @Test
    @DisplayName("Should handle 'initialize' JSON-RPC method successfully")
    void testInitializeMethod() {
        McpRequest request = McpRequest.builder()
                .id(1)
                .method("initialize")
                .build();

        McpResponse response = toolRegistry.handleRequest(request);

        assertThat(response.getError()).isNull();
        assertThat(response.getResult()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertThat(result).containsKey("protocolVersion");
        assertThat(result).containsKey("serverInfo");
    }

    @Test
    @DisplayName("Should execute db_introspect_schema tool and return enterprise tables")
    void testExecuteIntrospectSchema() {
        McpRequest request = McpRequest.builder()
                .id(2)
                .method("tools/call")
                .params(Map.of(
                        "name", "db_introspect_schema",
                        "arguments", Map.of("table_pattern", "%")
                ))
                .build();

        McpResponse response = toolRegistry.handleRequest(request);

        assertThat(response.getError()).isNull();
        assertThat(response.getResult()).isInstanceOf(McpToolResult.class);

        McpToolResult toolResult = (McpToolResult) response.getResult();
        assertThat(toolResult.isError()).isFalse();
        assertThat(toolResult.getContent()).isNotEmpty();

        String jsonText = toolResult.getContent().get(0).getText();
        assertThat(jsonText).contains("DEPARTMENTS");
        assertThat(jsonText).contains("EMPLOYEES");
    }

    @Test
    @DisplayName("Should execute db_execute_safe_query tool and return records")
    void testExecuteSafeQuery() {
        McpRequest request = McpRequest.builder()
                .id(3)
                .method("tools/call")
                .params(Map.of(
                        "name", "db_execute_safe_query",
                        "arguments", Map.of("query", "SELECT name, code FROM departments ORDER BY id ASC")
                ))
                .build();

        McpResponse response = toolRegistry.handleRequest(request);

        assertThat(response.getError()).isNull();
        McpToolResult toolResult = (McpToolResult) response.getResult();
        assertThat(toolResult.isError()).isFalse();

        String jsonText = toolResult.getContent().get(0).getText();
        assertThat(jsonText).contains("Engineering & AI Systems");
        assertThat(jsonText).contains("rows_returned");
    }

    @Test
    @DisplayName("Should execute system_get_telemetry tool and return JVM stats")
    void testExecuteSystemTelemetry() {
        McpRequest request = McpRequest.builder()
                .id(4)
                .method("tools/call")
                .params(Map.of(
                        "name", "system_get_telemetry",
                        "arguments", Map.of()
                ))
                .build();

        McpResponse response = toolRegistry.handleRequest(request);

        assertThat(response.getError()).isNull();
        McpToolResult toolResult = (McpToolResult) response.getResult();
        assertThat(toolResult.isError()).isFalse();

        String jsonText = toolResult.getContent().get(0).getText();
        assertThat(jsonText).contains("heap_used_mb");
        assertThat(jsonText).contains("uptime_seconds");
    }
}
