package com.farias.sentinel.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContainerService {

    private final DockerClient dockerClient;

    // Spring inyecta automaticamente el DockerClient configurado
    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void listarcontenedores() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true) // queremos ver los que estan prendidos y apagados
                .exec();

        System.out.println("--- Lista de contenedores Detectados ---");
        for (Container container : containers) {
            System.out.println("Nombre: " + container.getNames()[0] + " | Estado: " + container.getState());
        }
    }

    @Scheduled(fixedRate = 10000) // se ejecuta cada 10 segundos
    public void monitorearContenedores() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        System.out.println("Vigilando infraestructura...");

        for (Container container : containers) {
            String nombre = container.getNames()[0];
            String estado = container.getState();

            // solo nos interesa vigilar a nuestro paciente
            if (nombre.equals("/paciente-uno")) {
                if (!estado.equals("running")) {
                    System.err.println("¡ALERTA! " + nombre + " está en estado: " + estado);
                    revivirContenedor(container.getId());
                } else {
                    System.out.println(nombre + " esta saludable.");
                }
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
