package com.farias.sentinel.service;

import com.farias.sentinel.dto.ContainerStatusDTO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContainerService {

    private final DockerClient dockerClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final SlackService slackService;
    private final SentinelEventService sentinelEventService;

    public ContainerService(DockerClient dockerClient,
                            SimpMessagingTemplate messagingTemplate,
                            SlackService slackService,
                            SentinelEventService sentinelEventService) {
        this.dockerClient = dockerClient;
        this.messagingTemplate = messagingTemplate;
        this.slackService = slackService;
        this.sentinelEventService = sentinelEventService;
    }

    @Scheduled(fixedRate = 5000)
    public void monitorearContenedores() {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container container : containers) {
            String estado = container.getState();

            if (estado.equalsIgnoreCase("created")) continue;

            String nombre = container.getNames()[0].replace("/", "");
            String containerId = container.getId();

            java.util.Map<String, String> labels = container.getLabels();
            boolean esProtegido = labels.containsKey("sentinel.auto-heal") &&
                    labels.get("sentinel.auto-heal").equals("true");

            // Enviamos los datos al Dashboard
            messagingTemplate.convertAndSend("/topic/containers",
                    new ContainerStatusDTO(nombre, estado, containerId, esProtegido));

            // Lógica de Self-Healing y Registro de Eventos
            if (esProtegido && estado.equalsIgnoreCase("exited")) {

                // 1. Registramos la falla en la Base de Datos
                sentinelEventService.registrarEvento(
                        nombre,
                        containerId,
                        "FAILURE",
                        "Detección de estado 'exited'. Iniciando recuperación automática."
                );

                // 2. Intentamos revivir el contenedor
                revivirContenedor(containerId, nombre);
            }
        }
    }

    private void revivirContenedor(String containerId, String nombre) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();

            // 3. Registramos el éxito de la recuperación en la DB
            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "RECOVERY",
                    "Sentinel restauró el servicio exitosamente."
            );

            // Notificación a Slack
            String alerta = "*Sentinel Self-Healing Report*\n" +
                    "Contenedor restaurado: `" + nombre + "`\n" +
                    "ID: `" + containerId.substring(0, 12) + "`\n" +
                    "Estado: Disponibilidad recuperada automáticamente.";
            slackService.enviarNotificacion(alerta);

        } catch (Exception e) {
            // 4. Registramos el error crítico si el reinicio falla
            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "CRITICAL_ERROR",
                    "Falló el intento de reinicio: " + e.getMessage()
            );

            slackService.enviarNotificacion("*ERROR:* Falló el intento de reinicio en `" + nombre + "`");
        }
    }
}
