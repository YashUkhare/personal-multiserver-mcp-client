package com.mcp.client.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitializeRequest {
    private String protocolVersion = "2024-11-05";
    private ClientInfo clientInfo;
    private Capabilities capabilities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        private String name;
        private String version;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capabilities {
        private Map<String, Object> roots;
        private Map<String, Object> sampling;
    }

    public InitializeRequest(String clientName, String clientVersion) {
        this.clientInfo = new ClientInfo(clientName, clientVersion);
        this.capabilities = new Capabilities(Map.of(), Map.of());
    }
}