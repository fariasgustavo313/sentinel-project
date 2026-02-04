package com.farias.sentinel.controller;

import com.farias.sentinel.config.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin("*")
public class ContainerController {

    private final DockerClient dockerClient;

    public  ContainerController(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @PostMapping("/{id}/stop")
    public void stopcontainer(@PathVariable String id) {
        System.out.println("Solicitud manual de STOP para: " + id);
        try {
            dockerClient.stopContainerCmd(id).exec();
        } catch (Exception e) {
            System.err.println("Error al deneter: " + e.getMessage());
        }
    }
}
