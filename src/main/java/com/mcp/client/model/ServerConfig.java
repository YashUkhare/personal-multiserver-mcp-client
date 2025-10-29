package com.mcp.client.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfig {
    private String id;
    private String command;
    private List<String> args;
    private String workingDirectory;
}