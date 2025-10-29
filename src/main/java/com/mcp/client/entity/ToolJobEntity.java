package com.mcp.client.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tool_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serverId;
    private String toolName;

    @Column(length = 4000)
    private String argumentsJson;

    @Column(length = 8000)
    private String resultJson;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum Status {
        PENDING, RUNNING, SUCCESS, FAILED
    }
}
