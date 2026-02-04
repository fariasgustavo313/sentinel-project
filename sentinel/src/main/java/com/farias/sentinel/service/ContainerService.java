package com.farias.sentinel.service;

import com.farias.sentinel.dto.ContainerStatusDTO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ContainerService {

    private final DockerClient dockerClient;
    private final SimpMessagingTemplate messagingTemplate;

    public ContainerService(DockerClient dockerClient, SimpMessagingTemplate messagingTemplate) {
        this.dockerClient = dockerClient;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 5000)
    public void monitorearContenedores() {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container container : containers) {
            String nombre = container.getNames()[0];
            String estado = container.getState();

            // 1. Obtenemos las etiquetas del contenedor
            java.util.Map<String, String> labels = container.getLabels();

            // 2. Enviamos los datos al Dashboard (todos se ven, pero no todos se curan)
            messagingTemplate.convertAndSend("/topic/containers",
                    new ContainerStatusDTO(nombre, estado, container.getId()));

            // 3. Lógica de Self-Healing Selectiva
            // Solo revivimos si tiene la etiqueta y no está corriendo
            if (labels.containsKey("sentinel.auto-heal") &&
                    labels.get("sentinel.auto-heal").equals("true") &&
                    !estado.equals("running")) {

                revivirContenedor(container.getId());
            }
        }
    }

    private void revivirContenedor(String containerId) {
        System.out.println("Intentando revivir el contenedor: " + containerId + "...");
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            System.out.println("Contenedor reiniciado con exito!");
        } catch (Exception e) {
            System.err.println("Error al intentar reiniciar: " + e.getMessage());
        }
    }
}
