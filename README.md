# MCP Multi-Server Client - Spring Boot

A production-ready Spring Boot application that connects to multiple Model Context Protocol (MCP) servers, allowing you to manage and interact with various AI tools and resources.

## 🚀 Features

- ✅ Connect to multiple MCP servers simultaneously
- ✅ RESTful API for server management
- ✅ **Interactive Swagger/OpenAPI documentation**
- ✅ Dynamic tool discovery and invocation
- ✅ Resource listing and management
- ✅ Graceful connection handling and error management
- ✅ Health monitoring and status checks
- ✅ Comprehensive logging with SLF4J
- ✅ Clean architecture with separation of concerns

## 📋 Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Node.js 18+** (for MCP servers)
- **npm** (comes with Node.js)

## 🏗️ Project Structure

```
mcp-client/
├── src/
│   ├── main/
│   │   ├── java/com/example/mcpclient/
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   └── McpController.java
│   │   │   ├── model/
│   │   │   │   ├── JsonRpcRequest.java
│   │   │   │   ├── JsonRpcResponse.java
│   │   │   │   ├── McpTool.java
│   │   │   │   ├── McpResource.java
│   │   │   │   ├── ServerConfig.java
│   │   │   │   ├── InitializeRequest.java
│   │   │   │   └── ToolCallRequest.java
│   │   │   ├── service/
│   │   │   │   ├── McpClientService.java
│   │   │   │   └── McpServerConnection.java
│   │   │   └── McpClientApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/example/mcpclient/
└── pom.xml
```

## 🔧 Installation

### 1. Clone or Create the Project

```bash
mkdir mcp-client
cd mcp-client
```

### 2. Copy All Java Files

Copy all the Java files from the artifacts above into their respective packages:

- Config files → `src/main/java/com/example/mcpclient/config/`
  - OpenApiConfig.java
- Controller files → `src/main/java/com/example/mcpclient/controller/`
- Model files → `src/main/java/com/example/mcpclient/model/`
- Service files → `src/main/java/com/example/mcpclient/service/`
- Main application → `src/main/java/com/example/mcpclient/`

### 3. Copy Configuration Files

Copy `application.yml` to `src/main/resources/`

### 4. Copy pom.xml

Copy the `pom.xml` to the project root

### 5. Build the Project

```bash
mvn clean install
```

## 🚀 Running the Application

### Start the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Verify It's Running

```bash
curl http://localhost:8080/api/mcp/health
```

Expected output:
```json
{
  "status": "UP",
  "totalServers": 0,
  "connectedServers": 0
}
```

## 📚 API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation at:

**Swagger UI**: http://localhost:8080/swagger-ui.html

**OpenAPI JSON**: http://localhost:8080/api-docs

The Swagger UI provides:
- ✅ Interactive API testing
- ✅ Request/response examples
- ✅ Schema documentation
- ✅ Try it out functionality
- ✅ Authentication testing (if configured)

### Server Management

#### Register a Server
```bash
POST /api/mcp/servers
Content-Type: application/json

{
  "id": "memory-server",
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-memory"]
}
```

#### List All Servers
```bash
GET /api/mcp/servers
```

#### Get Server Status
```bash
GET /api/mcp/servers/{serverId}/status
```

#### Unregister a Server
```bash
DELETE /api/mcp/servers/{serverId}
```

### Tool Management

#### List Tools from a Server
```bash
GET /api/mcp/servers/{serverId}/tools
```

#### List Tools from All Servers
```bash
GET /api/mcp/tools
```

#### Call a Tool
```bash
POST /api/mcp/servers/{serverId}/tools/call
Content-Type: application/json

{
  "name": "store_memory",
  "arguments": {
    "key": "user_name",
    "value": "John Doe"
  }
}
```

### Resource Management

#### List Resources from a Server
```bash
GET /api/mcp/servers/{serverId}/resources
```

#### List Resources from All Servers
```bash
GET /api/mcp/resources
```

## 🔌 Available MCP Servers

Here are some MCP servers you can connect to:

### 1. Memory Server
```json
{
  "id": "memory-server",
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-memory"]
}
```
**Tools**: `store_memory`, `retrieve_memory`, `delete_memory`

### 2. Filesystem Server
```json
{
  "id": "filesystem-server",
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
}
```
**Tools**: `read_file`, `write_file`, `list_directory`

### 3. Time Server
```json
{
  "id": "time-server",
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-time"]
}
```
**Tools**: `get_current_time`, `get_timezone`

### 4. GitHub Server
```json
{
  "id": "github-server",
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-github"],
  "env": {
    "GITHUB_TOKEN": "your_github_token"
  }
}
```
**Tools**: Repository operations, issue management, etc.

## 💡 Usage Examples

### Example 1: Store and Retrieve Data

```bash
# Register memory server
curl -X POST http://localhost:8080/api/mcp/servers \
  -H "Content-Type: application/json" \
  -d '{"id":"memory","command":"npx","args":["-y","@modelcontextprotocol/server-memory"]}'

# Store data
curl -X POST http://localhost:8080/api/mcp/servers/memory/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"store_memory","arguments":{"key":"name","value":"Alice"}}'

# Retrieve data
curl -X POST http://localhost:8080/api/mcp/servers/memory/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"retrieve_memory","arguments":{"key":"name"}}'
```

### Example 2: File Operations

```bash
# Register filesystem server
curl -X POST http://localhost:8080/api/mcp/servers \
  -H "Content-Type: application/json" \
  -d '{"id":"fs","command":"npx","args":["-y","@modelcontextprotocol/server-filesystem","/tmp"]}'

# List available tools
curl http://localhost:8080/api/mcp/servers/fs/tools

# Read a file
curl -X POST http://localhost:8080/api/mcp/servers/fs/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"read_file","arguments":{"path":"/tmp/test.txt"}}'
```

## 🔍 Monitoring & Health Checks

### Application Health
```bash
curl http://localhost:8080/api/mcp/health
```

### Spring Boot Actuator Endpoints
```bash
# Health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

## ⚙️ Configuration

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: mcp-client

mcp:
  client:
    name: "spring-mcp-client"
    version: "1.0.0"

logging:
  level:
    com.example.mcpclient: DEBUG
```

### Custom Port

Change the port in `application.yml` or use command line:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 🐛 Troubleshooting

### Issue: "Server connection failed"
**Solution**: Ensure Node.js and npx are installed
```bash
node --version
npx --version
```

### Issue: "Port 8080 already in use"
**Solution**: Change port in application.yml or kill the process
```bash
lsof -ti:8080 | xargs kill -9
```

### Issue: "Tools not found"
**Solution**: Check server connection status
```bash
curl http://localhost:8080/api/mcp/servers/{serverId}/status
```

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Manual Testing with curl
See the Testing Guide artifact for comprehensive test scenarios.

## 📦 Building for Production

### Create JAR
```bash
mvn clean package
```

### Run JAR
```bash
java -jar target/mcp-client-1.0.0.jar
```

### Docker Support (Optional)

Create `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/mcp-client-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t mcp-client .
docker run -p 8080:8080 mcp-client
```

## 🔐 Security Considerations

1. **Add Authentication**: Implement Spring Security for API endpoints
2. **Validate Input**: Add request validation using Bean Validation
3. **Rate Limiting**: Implement rate limiting for API calls
4. **CORS**: Configure CORS for web clients
5. **Secrets Management**: Use environment variables for sensitive data

## 🚀 Future Enhancements

- [ ] WebSocket support for real-time notifications
- [ ] Database integration for persistent server configs
- [ ] Admin UI dashboard
- [ ] Metrics and monitoring with Prometheus
- [ ] Docker Compose setup
- [ ] Kubernetes deployment configs
- [ ] OpenAPI/Swagger documentation
- [ ] Batch operations support
- [ ] Server health monitoring
- [ ] Connection pooling

## 📝 License

MIT License

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues and questions:
- Check the Testing Guide artifact
- Review application logs
- Ensure all prerequisites are met

## 🎯 Quick Start Checklist

- [ ] Java 17+ installed
- [ ] Maven installed
- [ ] Node.js 18+ installed
- [ ] All Java files copied to correct packages (including OpenApiConfig.java)
- [ ] application.yml in src/main/resources
- [ ] pom.xml in project root (with Swagger dependency)
- [ ] Run `mvn clean install`
- [ ] Run `mvn spring-boot:run`
- [ ] Test health endpoint: `curl http://localhost:8080/api/mcp/health`
- [ ] **Open Swagger UI: http://localhost:8080/swagger-ui.html**
- [ ] Register first server via Swagger's "Try it out" button
- [ ] Start building!

---

**Built with ❤️ using Spring Boot, Model Context Protocol, and Swagger/OpenAPI**
