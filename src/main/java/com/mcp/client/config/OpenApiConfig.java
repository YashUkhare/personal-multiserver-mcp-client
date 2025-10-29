package com.mcp.client.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${mcp.client.version}")
    private String version;

    @Bean
    public OpenAPI mcpClientOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Multi-Server Client API")
                        .description("""
                                REST API for managing multiple Model Context Protocol (MCP) servers.

                                ## Features
                                - Connect to multiple MCP servers simultaneously
                                - Discover and invoke tools from connected servers
                                - List and access resources from servers
                                - Real-time server health monitoring

                                ## Getting Started
                                1. Register a server using POST /api/mcp/servers
                                2. List available tools using GET /api/mcp/servers/{serverId}/tools
                                3. Call tools using POST /api/mcp/servers/{serverId}/tools/call

                                ## Example MCP Servers
                                - Memory Server: `npx -y @modelcontextprotocol/server-memory`
                                - Filesystem Server: `npx -y @modelcontextprotocol/server-filesystem /tmp`
                                - Time Server: `npx -y @modelcontextprotocol/server-time`
                                """)
                        .version(version)
                        .contact(new Contact()
                                .name("MCP Client Support")
                                .email("support@example.com")
                                .url("https://modelcontextprotocol.io"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production Server")));
    }
}