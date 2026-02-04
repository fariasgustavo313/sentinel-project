package com.farias.sentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContainerStatusDTO {

    private String nombre;
    private String estado;
    private String id;
    private boolean protegido;
}
