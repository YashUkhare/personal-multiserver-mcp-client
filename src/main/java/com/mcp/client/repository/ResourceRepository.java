package com.mcp.client.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mcp.client.entity.ResourceEntity;

public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {
    List<ResourceEntity> findByServer_Id(String serverId);
}