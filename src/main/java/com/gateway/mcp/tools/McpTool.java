package com.gateway.mcp.tools;

import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;

import java.util.Map;

/**
 * Contract for Spring-managed Model Context Protocol (MCP) Tools.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
public interface McpTool {

    /**
     * Returns the tool schema definition advertised to AI models.
     */
    McpToolDefinition getDefinition();

    /**
     * Executes the tool logic given model-supplied arguments.
     *
     * @param arguments Dynamic argument map from AI JSON payload.
     * @return Execution result payload (text/resource blocks).
     */
    McpToolResult execute(Map<String, Object> arguments);
}
