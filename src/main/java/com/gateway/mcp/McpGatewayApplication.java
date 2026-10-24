package com.gateway.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Enterprise Model Context Protocol (MCP) Gateway Application.
 *
 * Bridges Enterprise Java 17 / Spring Boot 3 backends with Autonomous AI Agents
 * (Anthropic Claude Desktop, Cursor, OpenAI Agents, LangChain) via standard MCP protocols.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@SpringBootApplication
public class McpGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGatewayApplication.class, args);
    }
}
