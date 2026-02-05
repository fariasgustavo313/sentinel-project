package com.farias.sentinel.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class SentinelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String containerName;
    private String containerId;
    private String eventType; // FAILURE, RECOVERY, CRITICAL_STOP
    private LocalDateTime timestamp;
    private String details; // Ejemplo: reinicio automatico exitoso
}
