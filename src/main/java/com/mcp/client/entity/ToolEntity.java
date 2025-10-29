package com.mcp.client.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mcp_tools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 4000)
    private String inputSchema; // JSON string for schema

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private ServerEntity server;
}
