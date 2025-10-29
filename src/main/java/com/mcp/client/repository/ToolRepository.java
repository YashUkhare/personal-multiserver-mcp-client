package com.mcp.client.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mcp.client.entity.ToolEntity;

public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    List<ToolEntity> findByServer_Id(String serverId);
}
