package com.mcp.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.client.entity.ResourceEntity;
import com.mcp.client.entity.ServerEntity;
import com.mcp.client.entity.ToolEntity;
import com.mcp.client.entity.ToolJobEntity;
import com.mcp.client.model.*;
import com.mcp.client.repository.ResourceRepository;
import com.mcp.client.repository.ServerRepository;
import com.mcp.client.repository.ToolJobRepository;
import com.mcp.client.repository.ToolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {

    private final ServerRepository serverRepository;
    private final ToolRepository toolRepository;
    private final ResourceRepository resourceRepository;
    private final ToolJobRepository toolJobRepository;

    @Value("${mcp.client.name:spring-mcp-client}")
    private String clientName;

    @Value("${mcp.client.version:1.0.0}")
    private String clientVersion;

    private final Map<String, McpServerConnection> connections = new ConcurrentHashMap<>();

    // --------------- Restore Connections on Startup ---------------
    @PostConstruct
    public void restoreServers() {
        List<ServerEntity> entities = serverRepository.findAll();
        log.info("Restoring {} previously registered MCP servers...", entities.size());

        for (ServerEntity entity : entities) {
            try {
                ServerConfig config = new ServerConfig(
                        entity.getId(),
                        entity.getCommand(),
                        List.of(entity.getArgs().split(",")),
                        entity.getWorkingDirectory());

                McpServerConnection connection = new McpServerConnection(config);
                connection.connect(clientName, clientVersion);
                connections.put(entity.getId(), connection);

                entity.setStatus(ServerEntity.Status.CONNECTED);
                entity.setLastConnected(LocalDateTime.now());
                serverRepository.save(entity);
                log.info("Reconnected server: {}", entity.getId());

            } catch (IOException e) {
                log.warn("Failed to restore server {}: {}", entity.getId(), e.getMessage());
                entity.setStatus(ServerEntity.Status.FAILED);
                serverRepository.save(entity);
            }
        }
    }

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

        // Persist in DB
        ServerEntity entity = ServerEntity.builder()
                .id(config.getId())
                .command(config.getCommand())
                .args(String.join(",", config.getArgs()))
                .workingDirectory(config.getWorkingDirectory())
                .status(ServerEntity.Status.CONNECTED)
                .lastConnected(java.time.LocalDateTime.now())
                .build();

        serverRepository.save(entity);
        log.info("Successfully registered and persisted MCP server: {}", config.getId());
    }

    /**
     * Disconnect and unregister a server
     */
    public void unregisterServer(String serverId) {
        McpServerConnection connection = connections.remove(serverId);
        if (connection != null) {
            connection.disconnect();
            log.info("Unregistered MCP server: {}", serverId);

            serverRepository.findById(serverId).ifPresent(entity -> {
                entity.setStatus(ServerEntity.Status.DISCONNECTED);
                serverRepository.save(entity);
            });
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
        List<McpTool> tools = connection.listTools();

        // persist/update
        ServerEntity serverEntity = serverRepository.findById(serverId).orElseThrow();
        toolRepository.deleteAll(toolRepository.findByServer_Id(serverId)); // refresh existing
        for (McpTool tool : tools) {
            toolRepository.save(ToolEntity.builder()
                    .name(tool.getName())
                    .description(tool.getDescription())
                    .inputSchema(new ObjectMapper().writeValueAsString(tool.getInputSchema()))
                    .server(serverEntity)
                    .build());
        }

        return tools;
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
        List<McpResource> resources = connection.listResources();

        // persist/update
        ServerEntity serverEntity = serverRepository.findById(serverId).orElseThrow();
        resourceRepository.deleteAll(resourceRepository.findByServer_Id(serverId)); // refresh existing
        for (McpResource resource : resources) {
            resourceRepository.save(ResourceEntity.builder()
                    .name(resource.getName())
                    .description(resource.getDescription())
                    .server(serverEntity)
                    .uri(resource.getUri())
                    .mimeType(resource.getMimeType())
                    .build());
        }

        return resources;
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

    public void refreshAllServersData() {
        for (ServerEntity server : serverRepository.findAll()) {
            try {
                listTools(server.getId());
                listResources(server.getId());
            } catch (Exception e) {
                log.warn("Refresh failed for {}: {}", server.getId(), e.getMessage());
            }
        }
    }

    @Async
    public void executeToolJob(ToolJobEntity job) {
        try {
            job.setStatus(ToolJobEntity.Status.RUNNING);
            toolJobRepository.save(job);

            JsonNode result = callTool(job.getServerId(), job.getToolName(),
                    new ObjectMapper().readTree(job.getArgumentsJson()));

            job.setResultJson(result.toString());
            job.setStatus(ToolJobEntity.Status.SUCCESS);
        } catch (Exception e) {
            job.setResultJson("{\"error\":\"" + e.getMessage() + "\"}");
            job.setStatus(ToolJobEntity.Status.FAILED);
        } finally {
            job.setCompletedAt(LocalDateTime.now());
            toolJobRepository.save(job);
        }
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