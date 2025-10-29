package com.mcp.client.monitoring;

import com.mcp.client.service.McpClientService;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
public class McpServersHealthIndicator implements HealthIndicator {

    private final McpClientService service;

    public McpServersHealthIndicator(McpClientService service) {
        this.service = service;
    }

    @Override
    public Health health() {
        long connected = service.listServers().stream().filter(s -> s.isConnected()).count();
        long total = service.listServers().size();

        if (connected == total && total > 0)
            return Health.up().withDetail("connectedServers", connected).withDetail("totalServers", total).build();

        return Health.down()
                .withDetail("connectedServers", connected)
                .withDetail("totalServers", total)
                .build();
    }
}
