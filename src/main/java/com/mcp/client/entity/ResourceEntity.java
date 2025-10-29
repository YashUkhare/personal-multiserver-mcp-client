package com.mcp.client.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mcp_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uri;
    private String name;

    @Column(length = 2000)
    private String description;
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private ServerEntity server;
}
