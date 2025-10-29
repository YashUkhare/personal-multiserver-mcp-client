package com.mcp.client.repository;

import com.mcp.client.entity.ToolJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToolJobRepository extends JpaRepository<ToolJobEntity, Long> {
    List<ToolJobEntity> findByServerId(String serverId);
}
