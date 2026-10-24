package com.gateway.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.mcp.protocol.model.McpRequest;
import com.gateway.mcp.protocol.model.McpResponse;
import com.gateway.mcp.tools.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Standard I/O (stdio) Transport Runner for Desktop AI Clients (Claude Desktop, Cursor, IDEs).
 *
 * Reads JSON-RPC request lines from STDIN and emits JSON-RPC response lines to STDOUT.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.transport.stdio-enabled", havingValue = "true")
public class StdioMcpRunner implements CommandLineRunner {

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Stdio MCP Transport loop. Awaiting JSON-RPC requests on STDIN...");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    McpRequest request = objectMapper.readValue(line, McpRequest.class);
                    McpResponse response = toolRegistry.handleRequest(request);
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    System.out.println(jsonResponse);
                    System.out.flush();
                } catch (Exception ex) {
                    McpResponse errorResponse = McpResponse.error(null, -32700, "Parse error: " + ex.getMessage());
                    System.out.println(objectMapper.writeValueAsString(errorResponse));
                    System.out.flush();
                }
            }
        }
    }
}
