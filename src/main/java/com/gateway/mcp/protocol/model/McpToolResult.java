package com.gateway.mcp.protocol.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Model Context Protocol (MCP) Tool Execution Result payload.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResult {

    @JsonProperty("content")
    private List<ContentBlock> content;

    @JsonProperty("isError")
    @Builder.Default
    private boolean isError = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private String type = "text";

        @JsonProperty("text")
        private String text;
    }

    public static McpToolResult text(String text) {
        return McpToolResult.builder()
                .content(Collections.singletonList(ContentBlock.builder().type("text").text(text).build()))
                .isError(false)
                .build();
    }

    public static McpToolResult error(String errorMessage) {
        return McpToolResult.builder()
                .content(Collections.singletonList(ContentBlock.builder().type("text").text(errorMessage).build()))
                .isError(true)
                .build();
    }
}
