package com.farias.sentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainerStatusDTO {

    private String nombre;
    private String estado;
    private String id;
    private boolean protegido;
    private boolean blocked;
    private String cpuUsage;
    private String memUsage;
}
