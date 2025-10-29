package com.mcp.client.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "server_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerEntity {

    @Id
    @Column(length = 50)
    private String id;

    private String command;

    @Column(length = 1000)
    private String args; // Store args as JSON or comma-separated string

    private String workingDirectory;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime lastConnected;

    public enum Status {
        CONNECTED, DISCONNECTED, FAILED
    }
}
