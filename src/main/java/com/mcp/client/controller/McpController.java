package com.mcp.client.controller;

import com.mcp.client.entity.ResourceEntity;
import com.mcp.client.entity.ToolEntity;
import com.mcp.client.entity.ToolJobEntity;
import com.mcp.client.model.*;
import com.mcp.client.repository.ResourceRepository;
import com.mcp.client.repository.ToolJobRepository;
import com.mcp.client.repository.ToolRepository;
import com.mcp.client.service.McpClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Tag(name = "MCP Server Management", description = "APIs for managing MCP server connections and interactions")
public class McpController {

        private final McpClientService mcpClientService;
        private final ToolRepository toolRepository;
        private final ResourceRepository resourceRepository;
        private final ToolJobRepository toolJobRepository;

        /**
         * Register a new MCP server
         * POST /api/mcp/servers
         */
        @Operation(summary = "Register a new MCP server", description = "Connect to and register a new MCP server by providing its command and arguments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Server registered successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Server registered successfully\",\"serverId\":\"memory-server\"}"))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or server already registered", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\":\"Server with id memory-server already registered\"}"))),
                        @ApiResponse(responseCode = "500", description = "Failed to connect to server", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\":\"Failed to connect to server: connection timeout\"}")))
        })
        @PostMapping("/servers")
        public ResponseEntity<?> registerServer(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Server configuration including id, command, and arguments", required = true, content = @Content(schema = @Schema(implementation = ServerConfig.class), examples = @ExampleObject(name = "Memory Server", value = "{\"id\":\"memory-server\",\"command\":\"npx\",\"args\":[\"-y\",\"@modelcontextprotocol/server-memory\"]}"))) @RequestBody ServerConfig config) {
                try {
                        mcpClientService.registerServer(config);
                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Server registered successfully");
                        response.put("serverId", config.getId());
                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (IOException e) {
                        log.error("Failed to register server: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Failed to connect to server: " + e.getMessage()));
                }
        }

        /**
         * List all registered servers
         * GET /api/mcp/servers
         */
        @Operation(summary = "List all registered servers", description = "Get a list of all registered MCP servers with their connection status")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of servers retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = McpClientService.ServerInfo.class)))
        })
        @GetMapping("/servers")
        public ResponseEntity<List<McpClientService.ServerInfo>> listServers() {
                return ResponseEntity.ok(mcpClientService.listServers());
        }

        /**
         * Unregister a server
         * DELETE /api/mcp/servers/{serverId}
         */
        @Operation(summary = "Unregister a server", description = "Disconnect and remove a registered MCP server")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Server unregistered successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Server unregistered successfully\"}")))
        })
        @DeleteMapping("/servers/{serverId}")
        public ResponseEntity<?> unregisterServer(
                        @Parameter(description = "ID of the server to unregister", example = "memory-server") @PathVariable String serverId) {
                mcpClientService.unregisterServer(serverId);
                return ResponseEntity.ok(Map.of("message", "Server unregistered successfully"));
        }

        /**
         * Check if a server is connected
         * GET /api/mcp/servers/{serverId}/status
         */
        @Operation(summary = "Get server status", description = "Check if a specific MCP server is connected and operational")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Server status retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"serverId\":\"memory-server\",\"connected\":true}")))
        })
        @GetMapping("/servers/{serverId}/status")
        public ResponseEntity<?> getServerStatus(
                        @Parameter(description = "ID of the server", example = "memory-server") @PathVariable String serverId) {
                boolean connected = mcpClientService.isServerConnected(serverId);
                return ResponseEntity.ok(Map.of(
                                "serverId", serverId,
                                "connected", connected));
        }

        /**
         * List tools from a specific server
         * GET /api/mcp/servers/{serverId}/tools
         */
        @Operation(summary = "List tools from a server", description = "Get all available tools from a specific MCP server")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tools retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = McpTool.class))),
                        @ApiResponse(responseCode = "500", description = "Failed to list tools", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\":\"Server not found: memory-server\"}")))
        })
        @GetMapping("/servers/{serverId}/tools")
        public ResponseEntity<?> listTools(
                        @Parameter(description = "ID of the server", example = "memory-server") @PathVariable String serverId) {
                try {
                        List<McpTool> tools = mcpClientService.listTools(serverId);
                        return ResponseEntity.ok(tools);
                } catch (IOException e) {
                        log.error("Failed to list tools: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * List tools from all servers
         * GET /api/mcp/tools
         */
        @Operation(summary = "List tools from all servers", description = "Get all available tools from all registered MCP servers")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tools from all servers retrieved successfully", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/tools")
        public ResponseEntity<Map<String, List<McpTool>>> listAllTools() {
                return ResponseEntity.ok(mcpClientService.listAllTools());
        }

        /**
         * Call a tool on a specific server
         * POST /api/mcp/servers/{serverId}/tools/call
         */
        @Operation(summary = "Call a tool", description = "Invoke a specific tool on an MCP server with the provided arguments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tool executed successfully", content = @Content(mediaType = "application/json")),
                        @ApiResponse(responseCode = "500", description = "Tool execution failed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\":\"Tool not found: invalid_tool\"}")))
        })
        @PostMapping("/servers/{serverId}/tools/call")
        public ResponseEntity<?> callTool(
                        @Parameter(description = "ID of the server", example = "memory-server") @PathVariable String serverId,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tool call request with tool name and arguments", required = true, content = @Content(schema = @Schema(implementation = ToolCallRequest.class), examples = @ExampleObject(name = "Store Memory", value = "{\"name\":\"store_memory\",\"arguments\":{\"key\":\"user_name\",\"value\":\"John Doe\"}}"))) @RequestBody ToolCallRequest request) {
                try {
                        JsonNode result = mcpClientService.callTool(
                                        serverId,
                                        request.getName(),
                                        request.getArguments());
                        return ResponseEntity.ok(result);
                } catch (IOException e) {
                        log.error("Failed to call tool: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * List resources from a specific server
         * GET /api/mcp/servers/{serverId}/resources
         */
        @Operation(summary = "List resources from a server", description = "Get all available resources from a specific MCP server")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Resources retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = McpResource.class))),
                        @ApiResponse(responseCode = "500", description = "Failed to list resources", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/servers/{serverId}/resources")
        public ResponseEntity<?> listResources(
                        @Parameter(description = "ID of the server", example = "filesystem-server") @PathVariable String serverId) {
                try {
                        List<McpResource> resources = mcpClientService.listResources(serverId);
                        return ResponseEntity.ok(resources);
                } catch (IOException e) {
                        log.error("Failed to list resources: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * List resources from all servers
         * GET /api/mcp/resources
         */
        @Operation(summary = "List resources from all servers", description = "Get all available resources from all registered MCP servers")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Resources from all servers retrieved successfully", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/resources")
        public ResponseEntity<Map<String, List<McpResource>>> listAllResources() {
                return ResponseEntity.ok(mcpClientService.listAllResources());
        }

        /**
         * Health check endpoint
         * GET /api/mcp/health
         */
        @Operation(summary = "Health check", description = "Check the health status of the MCP client and connected servers")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Health status retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"status\":\"UP\",\"totalServers\":2,\"connectedServers\":2}")))
        })
        @GetMapping("/health")
        public ResponseEntity<?> health() {
                int totalServers = mcpClientService.listServers().size();
                long connectedServers = mcpClientService.listServers().stream()
                                .filter(McpClientService.ServerInfo::isConnected)
                                .count();

                return ResponseEntity.ok(Map.of(
                                "status", "UP",
                                "totalServers", totalServers,
                                "connectedServers", connectedServers));
        }

        @Operation(summary = "Refresh all tools and resources from servers")
        @PostMapping("/refresh")
        public ResponseEntity<?> refreshAll() {
                mcpClientService.refreshAllServersData();
                return ResponseEntity.ok(Map.of("message", "All servers refreshed successfully"));
        }

        @GetMapping("/tools/db")
        public ResponseEntity<List<ToolEntity>> getAllTools(
                        @RequestParam(required = false) String serverId,
                        @RequestParam(required = false) String name) {
                if (serverId != null)
                        return ResponseEntity.ok(toolRepository.findByServer_Id(serverId));
                if (name != null)
                        return ResponseEntity.ok(toolRepository.findAll()
                                        .stream()
                                        .filter(t -> t.getName().toLowerCase().contains(name.toLowerCase()))
                                        .toList());
                return ResponseEntity.ok(toolRepository.findAll());
        }

        @GetMapping("/resources/db")
        public ResponseEntity<List<ResourceEntity>> getAllResources(
                        @RequestParam(required = false) String serverId) {
                if (serverId != null)
                        return ResponseEntity.ok(resourceRepository.findByServer_Id(serverId));
                return ResponseEntity.ok(resourceRepository.findAll());
        }

        @PostMapping("/servers/{serverId}/tools/jobs")
        public ResponseEntity<?> createToolJob(
                        @PathVariable String serverId,
                        @RequestBody ToolCallRequest request) throws JsonProcessingException {
                try {
                        ToolJobEntity job = ToolJobEntity.builder()
                                        .serverId(serverId)
                                        .toolName(request.getName())
                                        .argumentsJson(new ObjectMapper().writeValueAsString(request.getArguments()))
                                        .status(ToolJobEntity.Status.PENDING)
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        job = toolJobRepository.save(job);
                        mcpClientService.executeToolJob(job);

                        return ResponseEntity.ok(Map.of("jobId", job.getId(), "status", job.getStatus()));
                } catch (JsonProcessingException e) {
                        return ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Failed to process job arguments: " + e.getMessage()));
                }
        }

        @GetMapping("/jobs/{id}")
        public ResponseEntity<ToolJobEntity> getJob(@PathVariable Long id) {
                return ResponseEntity.of(toolJobRepository.findById(id));
        }

        @GetMapping("/jobs")
        public ResponseEntity<List<ToolJobEntity>> listJobs(
                        @RequestParam(required = false) String serverId) {
                if (serverId != null)
                        return ResponseEntity.ok(toolJobRepository.findByServerId(serverId));
                return ResponseEntity.ok(toolJobRepository.findAll());
        }

}