# MCP Multi-Server Client (Spring Boot)

Spring Boot 3 service that connects to and orchestrates multiple Model Context Protocol (MCP) servers. It exposes a secure REST API for registering MCP servers, discovering tools and resources, invoking tools synchronously or via background jobs, and monitoring overall health.

## Features
- Manage the full life cycle of multiple MCP servers (register, auto-reconnect on startup, unregister).
- Discover tools and resources from each server and persist metadata in SQL Server.
- Invoke tools directly or queue asynchronous jobs backed by Spring `@Async` execution.
- JWT-based authentication with user registration and login endpoints.
- Global rate limiting (50 req/min) and centralized exception handling.
- Actuator health checks plus a custom MCP health indicator, Prometheus scraping, and structured logging.
- Interactive OpenAPI/Swagger UI for exploring the API.

## Tech Stack
- Java 21, Spring Boot 3.5.7
- Spring Web, Spring Validation, Spring Data JPA, Spring Security
- Microsoft SQL Server (via `mssql-jdbc`)
- Jackson for JSON-RPC handling, JJWT for tokens, Bucket4j for rate limiting
- springdoc-openapi, Micrometer + Prometheus registry

## Project Structure
- `src/main/java/com/mcp/client/ClientApplication.java`: application entry point (`@SpringBootApplication`, `@EnableAsync`).
- `config/`: security (`SecurityConfig`) and OpenAPI metadata.
- `controller/`: `/api/auth` and `/api/mcp` REST endpoints.
- `service/`: `McpClientService` (orchestration, persistence) and `McpServerConnection` (JSON-RPC bridge).
- `entity/` and `repository/`: JPA entities for servers, tools, resources, jobs, and users.
- `security/`: JWT generation/validation and the `UserDetailsService` adapter.
- `filter/RateLimitFilter`: Bucket4j servlet filter limiting traffic to 50 requests per minute.
- `monitoring/McpServersHealthIndicator`: contributes connected/total server counts to Actuator health.
- `model/`: JSON-RPC models and DTOs (`ServerConfig`, `ToolCallRequest`, etc.).

````text
client/
|-- README.md
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- .mvn/
|   |-- wrapper/
|       |-- maven-wrapper.jar
|       |-- maven-wrapper.properties
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   |-- com/mcp/client/
|   |   |       |-- ClientApplication.java
|   |   |       |-- config/
|   |   |       |   |-- OpenApiConfig.java
|   |   |       |   |-- SecurityConfig.java
|   |   |       |-- controller/
|   |   |       |   |-- AuthController.java
|   |   |       |   |-- McpController.java
|   |   |       |-- entity/
|   |   |       |   |-- ResourceEntity.java
|   |   |       |   |-- ServerEntity.java
|   |   |       |   |-- ToolEntity.java
|   |   |       |   |-- ToolJobEntity.java
|   |   |       |   |-- UserEntity.java
|   |   |       |-- exception/
|   |   |       |   |-- GlobalExceptionHandler.java
|   |   |       |-- filter/
|   |   |       |   |-- RateLimitFilter.java
|   |   |       |-- monitoring/
|   |   |       |   |-- McpServersHealthIndicator.java
|   |   |       |-- model/
|   |   |       |   |-- InitializeRequest.java
|   |   |       |   |-- JsonRpcRequest.java
|   |   |       |   |-- JsonRpcResponse.java
|   |   |       |   |-- McpResource.java
|   |   |       |   |-- McpTool.java
|   |   |       |   |-- ServerConfig.java
|   |   |       |   |-- ToolCallRequest.java
|   |   |       |-- repository/
|   |   |       |   |-- ResourceRepository.java
|   |   |       |   |-- ServerRepository.java
|   |   |       |   |-- ToolJobRepository.java
|   |   |       |   |-- ToolRepository.java
|   |   |       |   |-- UserRepository.java
|   |   |       |-- security/
|   |   |       |   |-- CustomUserDetailsService.java
|   |   |       |   |-- JwtAuthFilter.java
|   |   |       |   |-- JwtService.java
|   |   |       |-- service/
|   |   |           |-- McpClientService.java
|   |   |           |-- McpServerConnection.java
|   |   |-- resources/
|   |       |-- application.yml
|   |       |-- application.properties
|   |       |-- static/
|   |       |-- templates/
|   |-- test/
|       |-- java/
|           |-- com/mcp/client/
|               |-- ClientApplicationTests.java
|-- target/ (build output)
````

## Prerequisites
- JDK 21
- Maven 3.9+ (or use the bundled `mvnw.cmd`/`mvnw` wrapper)
- Microsoft SQL Server 2019+ (or Azure SQL) with a database named `mcp_client`
- Node.js 18+ and npm (only required when you want to run sample MCP servers via `npx`)

## Configuration
`src/main/resources/application.yml` reads from `.env` so you can tweak settings without touching committed config. Provide values for:

- `SERVER_PORT`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `MCP_CLIENT_NAME`, `MCP_CLIENT_VERSION`
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`

JPA is configured with `ddl-auto: update` for development and Swagger UI stays exposed at `/swagger-ui.html`. Actuator surfaces health, metrics, and Prometheus scraping endpoints out of the box. If any property is missing, Spring will fail fast, so make sure your `.env` (or environment) contains the complete set above.

Override any property via environment variables or JVM args, for example:

```bash
mvnw.cmd spring-boot:run ^
  -Dspring-boot.run.jvmArguments="
    -Dspring.datasource.url=jdbc:sqlserver://localhost:1433;database=mcp_client;encrypt=false
    -Dspring.datasource.username=sa
    -Dspring.datasource.password=ChangeMe!"
```

> **Security note:** replace the default datasource password and JWT signing key (`JwtService`) before deploying beyond local development.

## Build and Run
```bash
# Windows
mvnw.cmd clean package
mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw clean package
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`. To run the packaged jar:

```bash
java -jar target/client-0.0.1-SNAPSHOT.jar
```

## Running Sample MCP Servers
Use the official MCP sample servers to exercise the API:

```bash
npx -y @modelcontextprotocol/server-memory
npx -y @modelcontextprotocol/server-filesystem ./sandbox
npx -y @modelcontextprotocol/server-time
```

Register each server with its launch command so the client can spawn and manage the process.

## Authentication Workflow
1. **Register a user (open endpoint)**
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"StrongPass!","role":"ADMIN"}'
   ```
2. **Login and obtain a JWT**
   ```bash
   curl -X POST "http://localhost:8080/api/auth/login?username=admin&password=StrongPass!"
   ```
   The response contains `token`, `username`, and `role`.
3. **Call protected endpoints with the token**
   ```bash
   curl http://localhost:8080/api/mcp/servers \
     -H "Authorization: Bearer <token>"
   ```

Endpoints under `/api/auth/**`, `/swagger-ui/**`, and `/v3/api-docs/**` are public; everything else requires a valid Bearer token.

## Key REST Endpoints
| Method | Path | Description |
| ------ | ---- | ----------- |
| POST | `/api/mcp/servers` | Register and connect to a new MCP server (accepts `ServerConfig`). |
| GET | `/api/mcp/servers` | List registered servers with connection status. |
| GET | `/api/mcp/servers/{serverId}/status` | Check if a server connection is alive. |
| DELETE | `/api/mcp/servers/{serverId}` | Gracefully disconnect and unregister a server. |
| GET | `/api/mcp/servers/{serverId}/tools` | Fetch live tool definitions from a server; results are synced to the database. |
| GET | `/api/mcp/tools` | List tools aggregated across all connected servers. |
| POST | `/api/mcp/servers/{serverId}/tools/call` | Invoke a tool immediately with provided arguments. |
| POST | `/api/mcp/servers/{serverId}/tools/jobs` | Queue a background tool invocation (persisted in `tool_jobs`). |
| GET | `/api/mcp/jobs/{id}` | Retrieve job status and stored tool output. |
| GET | `/api/mcp/resources` | Aggregate resources across all servers (also persisted). |
| POST | `/api/mcp/refresh` | Refresh tool and resource caches for every server. |
| GET | `/api/mcp/health` | Lightweight health summary (total vs connected servers). |
| GET | `/swagger-ui.html` | Interactive OpenAPI documentation. |
| GET | `/actuator/health`, `/actuator/prometheus` | Spring Boot Actuator endpoints (prometheus requires Micrometer scrape). |

`ToolJobEntity` records automatically transition from `PENDING` -> `RUNNING` -> `SUCCESS/FAILED` as the async executor processes them. Responses include the stored JSON output or error payload.

## Persistence and Auto-Restart Behaviour
- Servers, tools, resources, jobs, and users are stored in SQL Server tables (`server_registry`, `mcp_tools`, `mcp_resources`, `tool_jobs`, `users`). Tables are created automatically (`ddl-auto: update`).
- On application startup, `McpClientService.restoreServers()` reconnects to every persisted server so tool/resource discovery continues without manual intervention.
- Tool/resource discovery wipes and repopulates the cached database entries per server to keep metadata in sync.
- Background jobs (`executeToolJob`) run asynchronously so the HTTP response returns immediately while long-running tool calls are processed.

## Monitoring and Operations
- **Rate limiting:** `RateLimitFilter` limits all requests to 50 per minute globally. Adjust the Bucket4j configuration to tune limits.
- **Actuator health:** `/actuator/health` includes MCP-specific details via `McpServersHealthIndicator` (connected/total counts).
- **Metrics:** `/actuator/prometheus` publishes Micrometer metrics ready for Prometheus scraping.
- **Logging:** SLF4J + Logback with package-level overrides configured in `application.yml`.

## Testing
```bash
mvnw.cmd test
```

The default test suite includes a Spring context smoke test. Add controller/service integration tests as you extend the application.

## Useful Commands
- Health probe: `curl http://localhost:8080/api/mcp/health`
- Register sample server:
  ```bash
  curl -X POST http://localhost:8080/api/mcp/servers \
    -H "Authorization: Bearer <token>" \
    -H "Content-Type: application/json" \
    -d '{ "id": "memory-server", "command": "npx", "args": ["-y","@modelcontextprotocol/server-memory"] }'
  ```
- List cached tools for a server: `curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/mcp/tools/db?serverId=memory-server"`

## Next Steps
1. Externalize sensitive secrets (datasource password, JWT key) into a vault or environment variables.
2. Harden authentication (longer token expiry, refresh tokens, auditing).
3. Add integration tests that exercise live MCP servers (memory/time/filesystem) via Testcontainers.
4. Package the service with Docker or Kubernetes manifests for deployment.

## License
MIT License

