package com.mcp.client.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class McpResource {
    private String uri;
    private String name;
    private String description;
    private String mimeType;
}
