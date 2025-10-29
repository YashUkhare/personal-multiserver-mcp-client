package com.mcp.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.client.model.*;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class McpServerConnection {
    private final ServerConfig config;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    
    private Process serverProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean connected = false;

    public McpServerConnection(ServerConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public void connect(String clientName, String clientVersion) throws IOException {
        log.info("Connecting to MCP server: {}", config.getId());
        
        try {
            // Build the command
            List<String> command = new ArrayList<>();
            command.add(config.getCommand());
            if (config.getArgs() != null) {
                command.addAll(config.getArgs());
            }

            // Start the server process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (config.getWorkingDirectory() != null) {
                processBuilder.directory(new File(config.getWorkingDirectory()));
            }
            processBuilder.redirectErrorStream(true);
            
            serverProcess = processBuilder.start();
            
            // Setup IO streams
            reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
            
            // Send initialize request
            InitializeRequest initRequest = new InitializeRequest(clientName, clientVersion);
            JsonRpcRequest request = new JsonRpcRequest(
                requestIdCounter.getAndIncrement(),
                "initialize",
                initRequest
            );
            
            JsonRpcResponse response = sendRequest(request);
            
            if (response.getError() != null) {
                throw new IOException("Failed to initialize: " + response.getError().getMessage());
            }
            
            // Send initialized notification
            JsonRpcRequest initializedNotification = new JsonRpcRequest();
            initializedNotification.setMethod("notifications/initialized");
            sendNotification(initializedNotification);
            
            connected = true;
            log.info("Successfully connected to MCP server: {}", config.getId());
            
        } catch (IOException e) {
            cleanup();
            throw new IOException("Failed to connect to MCP server: " + e.getMessage(), e);
        }
    }

    public JsonRpcResponse sendRequest(JsonRpcRequest request) throws IOException {
        if (!connected && !"initialize".equals(request.getMethod())) {
            throw new IOException("Server not connected");
        }

        try {
            // Send request
            String jsonRequest = objectMapper.writeValueAsString(request);
            log.debug("Sending request to {}: {}", config.getId(), jsonRequest);
            
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            // Read response
            String responseLine = reader.readLine();
            if (responseLine == null) {
                throw new IOException("Server closed connection");
            }
            
            log.debug("Received response from {}: {}", config.getId(), responseLine);
            
            JsonRpcResponse response = objectMapper.readValue(responseLine, JsonRpcResponse.class);
            return response;
            
        } catch (IOException e) {
            log.error("Error communicating with server {}: {}", config.getId(), e.getMessage());
            throw e;
        }
    }

    private void sendNotification(JsonRpcRequest notification) throws IOException {
        String jsonNotification = objectMapper.writeValueAsString(notification);
        log.debug("Sending notification to {}: {}", config.getId(), jsonNotification);
        
        writer.write(jsonNotification);
        writer.newLine();
        writer.flush();
    }

    public List<McpTool> listTools() throws IOException {
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "tools/list",
            null
        );
        
        JsonRpcResponse response = sendRequest(request);
        
        if (response.getError() != null) {
            throw new IOException("Failed to list tools: " + response.getError().getMessage());
        }
        
        JsonNode resultNode = objectMapper.convertValue(response.getResult(), JsonNode.class);
        JsonNode toolsNode = resultNode.get("tools");
        
        List<McpTool> tools = new ArrayList<>();
        if (toolsNode != null && toolsNode.isArray()) {
            for (JsonNode toolNode : toolsNode) {
                McpTool tool = objectMapper.treeToValue(toolNode, McpTool.class);
                tools.add(tool);
            }
        }
        
        return tools;
    }

    public JsonNode callTool(String toolName, Object arguments) throws IOException {
        ToolCallRequest toolCallRequest = new ToolCallRequest(toolName, 
            arguments != null ? objectMapper.convertValue(arguments, objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class)) : null);
        
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "tools/call",
            toolCallRequest
        );
        
        JsonRpcResponse response = sendRequest(request);
        
        if (response.getError() != null) {
            throw new IOException("Failed to call tool: " + response.getError().getMessage());
        }
        
        return objectMapper.convertValue(response.getResult(), JsonNode.class);
    }

    public List<McpResource> listResources() throws IOException {
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "resources/list",
            null
        );
        
        JsonRpcResponse response = sendRequest(request);
        
        if (response.getError() != null) {
            throw new IOException("Failed to list resources: " + response.getError().getMessage());
        }
        
        JsonNode resultNode = objectMapper.convertValue(response.getResult(), JsonNode.class);
        JsonNode resourcesNode = resultNode.get("resources");
        
        List<McpResource> resources = new ArrayList<>();
        if (resourcesNode != null && resourcesNode.isArray()) {
            for (JsonNode resourceNode : resourcesNode) {
                McpResource resource = objectMapper.treeToValue(resourceNode, McpResource.class);
                resources.add(resource);
            }
        }
        
        return resources;
    }

    public void disconnect() {
        log.info("Disconnecting from MCP server: {}", config.getId());
        cleanup();
    }

    private void cleanup() {
        connected = false;
        
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            log.warn("Error closing writer: {}", e.getMessage());
        }
        
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            log.warn("Error closing reader: {}", e.getMessage());
        }
        
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serverProcess.destroyForcibly();
            }
        }
    }

    public boolean isConnected() {
        return connected && serverProcess != null && serverProcess.isAlive();
    }

    public ServerConfig getConfig() {
        return config;
    }
}