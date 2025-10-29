package com.mcp.client.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
