package com.mcp.client.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallRequest {
    private String name;
    private Map<String, Object> arguments;
}