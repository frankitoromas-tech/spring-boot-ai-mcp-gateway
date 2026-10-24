package com.gateway.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.mcp.protocol.model.McpToolDefinition;
import com.gateway.mcp.protocol.model.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enterprise JVM & System Telemetry Tool for AI Agents.
 *
 * Exposes live memory health (Heap / Non-Heap), thread contention,
 * and application uptime.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
@Slf4j
@Component
public class SystemMetricsTool implements McpTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return McpToolDefinition.builder()
                .name("system_get_telemetry")
                .description("Retrieves live JVM heap utilization, active threads, available processors, and system uptime.")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Collections.emptyMap()
                ))
                .build();
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            long heapUsedBytes = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMaxBytes = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommittedBytes = memoryBean.getHeapMemoryUsage().getCommitted();

            Map<String, Object> telemetry = new LinkedHashMap<>();
            telemetry.put("uptime_seconds", runtimeBean.getUptime() / 1000.0);
            telemetry.put("available_processors", Runtime.getRuntime().availableProcessors());
            telemetry.put("active_threads", threadBean.getThreadCount());
            telemetry.put("peak_threads", threadBean.getPeakThreadCount());

            Map<String, Object> memory = new LinkedHashMap<>();
            memory.put("heap_used_mb", Math.round(heapUsedBytes / (1024.0 * 1024.0) * 100.0) / 100.0);
            memory.put("heap_committed_mb", Math.round(heapCommittedBytes / (1024.0 * 1024.0) * 100.0) / 100.0);
            memory.put("heap_max_mb", Math.round(heapMaxBytes / (1024.0 * 1024.0) * 100.0) / 100.0);
            memory.put("heap_utilization_percent", Math.round(((double) heapUsedBytes / heapMaxBytes) * 10000.0) / 100.0);

            telemetry.put("jvm_memory", memory);

            return McpToolResult.text(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(telemetry));

        } catch (Exception ex) {
            log.error("Failed to gather system metrics: {}", ex.getMessage(), ex);
            return McpToolResult.error("Failed to gather system telemetry: " + ex.getMessage());
        }
    }
}
