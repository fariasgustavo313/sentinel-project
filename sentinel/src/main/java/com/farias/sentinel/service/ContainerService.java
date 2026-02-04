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
    private final SlackService slackService;

    public ContainerService(DockerClient dockerClient, SimpMessagingTemplate messagingTemplate, SlackService slackService) {
        this.dockerClient = dockerClient;
        this.messagingTemplate = messagingTemplate;
        this.slackService = slackService;
    }

    @Scheduled(fixedRate = 5000)
    public void monitorearContenedores() {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container container : containers) {
            String estado = container.getState();

            // si el contenedor esta recien creado, se ignora
            // hasta que tenga un estado (running o exited)
            if (estado.equalsIgnoreCase("created")) continue;

            String nombre = container.getNames()[0].replace("/", "");
            // Obtenemos las etiquetas del contenedor
            java.util.Map<String, String> labels = container.getLabels();
            boolean esProtegido = labels.containsKey("sentinel.auto-heal") &&
                    labels.get("sentinel.auto-heal").equals("true");

            // Enviamos los datos al Dashboard con la marca de proteccion
            messagingTemplate.convertAndSend("/topic/containers",
                    new ContainerStatusDTO(nombre, estado, container.getId(), esProtegido));

            // logica de self-healting: solo si es protegido y esta realmente caido
            if (esProtegido && estado.equalsIgnoreCase("exited")) {
                revivirContenedor(container.getId(), nombre);
            }
        }
    }

    private void revivirContenedor(String containerId, String nombre) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            // notificacion a Slack
            String alerta = "*Sentinel Self-Healing Report*\n" +
                    "Contenedor restaurado: `" + nombre + "`\n" +
                    "ID: `" + containerId.substring(0, 12) + "`\n" +
                    "Estado: Disponibilidad recuperada automáticamente.";
            slackService.enviarNotificacion(alerta);
        } catch (Exception e) {
            slackService.enviarNotificacion("*ERROR:* Falló el intento de reinicio en `" + nombre + "`");
        }
    }
}
