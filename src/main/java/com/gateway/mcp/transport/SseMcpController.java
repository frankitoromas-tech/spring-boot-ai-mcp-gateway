package com.gateway.mcp.transport;

import com.gateway.mcp.protocol.model.McpRequest;
import com.gateway.mcp.protocol.model.McpResponse;
import com.gateway.mcp.tools.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Sent Events (SSE) and HTTP POST Transport for Model Context Protocol (MCP).
 *
 * Provides standard endpoints:
 * - GET /mcp/sse : Connects client and streams notifications.
 * - POST /mcp/message : Receives JSON-RPC requests from AI Agent / Client.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class SseMcpController {

    private final McpToolRegistry toolRegistry;
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        String sessionId = Long.toHexString(System.currentTimeMillis());

        activeEmitters.put(sessionId, emitter);

        emitter.onCompletion(() -> activeEmitters.remove(sessionId));
        emitter.onTimeout(() -> {
            emitter.complete();
            activeEmitters.remove(sessionId);
        });
        emitter.onError((e) -> activeEmitters.remove(sessionId));

        try {
            // Emit endpoint registration event per MCP SSE Specification
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/message?sessionId=" + sessionId));
        } catch (IOException e) {
            log.warn("Failed to send initial endpoint event to SSE client: {}", e.getMessage());
            emitter.complete();
            activeEmitters.remove(sessionId);
        }

        return emitter;
    }

    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpResponse> handleMessage(@RequestBody McpRequest request) {
        McpResponse response = toolRegistry.handleRequest(request);
        return ResponseEntity.ok(response);
    }
}
