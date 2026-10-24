package com.gateway.mcp.tools;

import com.gateway.mcp.protocol.model.McpRequest;
import com.gateway.mcp.protocol.model.McpResponse;
import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Model Context Protocol (MCP) Tool Registry and Dispatcher.
 *
 * Automatically registers all Spring-managed McpTool beans and handles
 * JSON-RPC protocol dispatch:
 * - 'initialize'
 * - 'tools/list'
 * - 'tools/call'
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolRegistry {

    private final List<McpTool> tools;
    private final Map<String, McpTool> toolMap = new ConcurrentHashMap<>();

    @Value("${mcp.server.name:Enterprise-Spring-MCP-Gateway}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    @PostConstruct
    public void init() {
        for (McpTool tool : tools) {
            String name = tool.getDefinition().getName();
            toolMap.put(name, tool);
            log.info("Registered MCP tool: [{}] -> {}", name, tool.getDefinition().getDescription());
        }
    }

    public List<McpToolDefinition> listToolDefinitions() {
        return tools.stream().map(McpTool::getDefinition).toList();
    }

    public McpResponse handleRequest(McpRequest request) {
        if (request == null || request.getMethod() == null) {
            return McpResponse.error(null, -32600, "Invalid Request: method is missing.");
        }

        Object id = request.getId();
        String method = request.getMethod();

        log.debug("Processing MCP RPC method: [{}]", method);

        switch (method) {
            case "initialize":
                return handleInitialize(id);

            case "notifications/initialized":
                return McpResponse.success(id, Map.of("status", "ready"));

            case "tools/list":
                return McpResponse.success(id, Map.of("tools", listToolDefinitions()));

            case "tools/call":
                return handleToolCall(id, request.getParams());

            case "ping":
                return McpResponse.success(id, Collections.emptyMap());

            default:
                log.warn("Unsupported MCP method: {}", method);
                return McpResponse.error(id, -32601, "Method not found: " + method);
        }
    }

    private McpResponse handleInitialize(Object id) {
        Map<String, Object> serverInfo = Map.of(
                "name", serverName,
                "version", serverVersion
        );

        Map<String, Object> capabilities = Map.of(
                "tools", Map.of("listChanged", false),
                "logging", Collections.emptyMap()
        );

        Map<String, Object> result = Map.of(
                "protocolVersion", "2024-11-05",
                "serverInfo", serverInfo,
                "capabilities", capabilities
        );

        return McpResponse.success(id, result);
    }

    @SuppressWarnings("unchecked")
    private McpResponse handleToolCall(Object id, Map<String, Object> params) {
        if (params == null || !params.containsKey("name")) {
            return McpResponse.error(id, -32602, "Invalid params: 'name' is required.");
        }

        String toolName = params.get("name").toString();
        McpTool tool = toolMap.get(toolName);

        if (tool == null) {
            return McpResponse.error(id, -32601, "Tool not found: " + toolName);
        }

        Map<String, Object> arguments = Collections.emptyMap();
        if (params.containsKey("arguments") && params.get("arguments") instanceof Map) {
            arguments = (Map<String, Object>) params.get("arguments");
        }

        try {
            McpToolResult toolResult = tool.execute(arguments);
            return McpResponse.success(id, toolResult);
        } catch (Exception ex) {
            log.error("Unhandled error executing tool '{}': {}", toolName, ex.getMessage(), ex);
            return McpResponse.success(id, McpToolResult.error("Execution error: " + ex.getMessage()));
        }
    }
}
