package com.mcp.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcp.client.model.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class McpClientService {

    @Value("${mcp.client.name:spring-mcp-client}")
    private String clientName;

    @Value("${mcp.client.version:1.0.0}")
    private String clientVersion;

    private final Map<String, McpServerConnection> connections = new ConcurrentHashMap<>();

    /**
     * Register and connect to a new MCP server
     */
    public void registerServer(ServerConfig config) throws IOException {
        if (connections.containsKey(config.getId())) {
            throw new IllegalArgumentException("Server with id " + config.getId() + " already registered");
        }

        log.info("Registering MCP server: {}", config.getId());
        
        McpServerConnection connection = new McpServerConnection(config);
        connection.connect(clientName, clientVersion);
        
        connections.put(config.getId(), connection);
        
        log.info("Successfully registered MCP server: {}", config.getId());
    }

    /**
     * Disconnect and unregister a server
     */
    public void unregisterServer(String serverId) {
        McpServerConnection connection = connections.remove(serverId);
        if (connection != null) {
            connection.disconnect();
            log.info("Unregistered MCP server: {}", serverId);
        }
    }

    /**
     * Get list of all registered servers
     */
    public List<ServerInfo> listServers() {
        List<ServerInfo> servers = new ArrayList<>();
        
        for (Map.Entry<String, McpServerConnection> entry : connections.entrySet()) {
            ServerInfo info = new ServerInfo();
            info.setId(entry.getKey());
            info.setConnected(entry.getValue().isConnected());
            info.setConfig(entry.getValue().getConfig());
            servers.add(info);
        }
        
        return servers;
    }

    /**
     * List tools available from a specific server
     */
    public List<McpTool> listTools(String serverId) throws IOException {
        McpServerConnection connection = getConnection(serverId);
        return connection.listTools();
    }

    /**
     * List tools from all servers
     */
    public Map<String, List<McpTool>> listAllTools() {
        Map<String, List<McpTool>> allTools = new HashMap<>();
        
        for (Map.Entry<String, McpServerConnection> entry : connections.entrySet()) {
            try {
                List<McpTool> tools = entry.getValue().listTools();
                allTools.put(entry.getKey(), tools);
            } catch (IOException e) {
                log.error("Error listing tools from server {}: {}", entry.getKey(), e.getMessage());
                allTools.put(entry.getKey(), new ArrayList<>());
            }
        }
        
        return allTools;
    }

    /**
     * Call a tool on a specific server
     */
    public JsonNode callTool(String serverId, String toolName, Object arguments) throws IOException {
        McpServerConnection connection = getConnection(serverId);
        return connection.callTool(toolName, arguments);
    }

    /**
     * List resources available from a specific server
     */
    public List<McpResource> listResources(String serverId) throws IOException {
        McpServerConnection connection = getConnection(serverId);
        return connection.listResources();
    }

    /**
     * List resources from all servers
     */
    public Map<String, List<McpResource>> listAllResources() {
        Map<String, List<McpResource>> allResources = new HashMap<>();
        
        for (Map.Entry<String, McpServerConnection> entry : connections.entrySet()) {
            try {
                List<McpResource> resources = entry.getValue().listResources();
                allResources.put(entry.getKey(), resources);
            } catch (IOException e) {
                log.error("Error listing resources from server {}: {}", entry.getKey(), e.getMessage());
                allResources.put(entry.getKey(), new ArrayList<>());
            }
        }
        
        return allResources;
    }

    /**
     * Check if a server is registered and connected
     */
    public boolean isServerConnected(String serverId) {
        McpServerConnection connection = connections.get(serverId);
        return connection != null && connection.isConnected();
    }

    /**
     * Get connection for a specific server
     */
    private McpServerConnection getConnection(String serverId) throws IOException {
        McpServerConnection connection = connections.get(serverId);
        if (connection == null) {
            throw new IOException("Server not found: " + serverId);
        }
        if (!connection.isConnected()) {
            throw new IOException("Server not connected: " + serverId);
        }
        return connection;
    }

    /**
     * Disconnect all servers on shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MCP client, disconnecting all servers");
        for (Map.Entry<String, McpServerConnection> entry : connections.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                log.error("Error disconnecting server {}: {}", entry.getKey(), e.getMessage());
            }
        }
        connections.clear();
    }

    /**
     * Inner class for server information
     */
    public static class ServerInfo {
        private String id;
        private boolean connected;
        private ServerConfig config;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public ServerConfig getConfig() {
            return config;
        }

        public void setConfig(ServerConfig config) {
            this.config = config;
        }
    }
}