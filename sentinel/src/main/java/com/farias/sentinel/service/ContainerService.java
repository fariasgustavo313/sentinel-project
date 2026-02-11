package com.farias.sentinel.service;

import com.farias.sentinel.dto.ContainerStatusDTO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContainerService {

    private final DockerClient dockerClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final SlackService slackService;
    private final SentinelEventService sentinelEventService;
    private final Map<String, LocalDateTime> alertasEnviadas = new ConcurrentHashMap<>();

    private final Map<String, Integer> retryTracker = new ConcurrentHashMap<>();
    private final Map<String, String[]> metricsCache = new ConcurrentHashMap<>(); // Guarda [CPU, RAM]
    private final Map<String, Closeable> activeStreams = new ConcurrentHashMap<>(); // Guarda las conexiones de Stats
    private static final int MAX_RETRIES = 3;

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
            String nombre = container.getNames()[0].replace("/", "");
            String containerId = container.getId();

            // Iniciar streaming de estadísticas si está corriendo y no lo estamos escuchando ya
            if (estado.equalsIgnoreCase("running") && !activeStreams.containsKey(containerId)) {
                iniciarStreamingDeMetricas(containerId);
            }

            Map<String, String> labels = container.getLabels();
            boolean esProtegido = labels.containsKey("sentinel.auto-heal") &&
                    labels.get("sentinel.auto-heal").equals("true");

            int intentos = retryTracker.getOrDefault(nombre, 0);
            boolean estaBloqueado = intentos > MAX_RETRIES;

            if (estado.equalsIgnoreCase("running") && intentos > 0) {
                retryTracker.remove(nombre);
                estaBloqueado = false;
            }

            // Obtener métricas del caché
            String[] metrics = metricsCache.getOrDefault(containerId, new String[]{"0.00%", "0MB"});

            messagingTemplate.convertAndSend("/topic/containers",
                    new ContainerStatusDTO(nombre, estado, containerId, esProtegido, estaBloqueado, metrics[0], metrics[1]));

            if (esProtegido && estado.equalsIgnoreCase("exited") && !estaBloqueado) {
                procesarFalla(containerId, nombre);
            }
        }
    }

    private void iniciarStreamingDeMetricas(String containerId) {
        Closeable stream = dockerClient.statsCmd(containerId).exec(new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {}

            @Override
            public void onNext(Statistics stats) {
                System.out.println("Datos recibidos para: " + containerId);
                String cpu = calcularCpu(stats);
                String mem = calcularMemoria(stats);
                metricsCache.put(containerId, new String[]{cpu, mem});

                // --- LÓGICA PROACTIVA ---
                if (stats.getMemoryStats() != null) {
                    long usageMB = stats.getMemoryStats().getUsage() / (1024 * 1024);

                    // Si supera los 500MB (Ajusta este valor según tu necesidad)
                    if (usageMB > 500) {
                        dispararAlertaProactiva(containerId, usageMB);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                activeStreams.remove(containerId);
            }

            @Override
            public void onComplete() {
                activeStreams.remove(containerId);
            }

            @Override
            public void close() throws IOException {
                activeStreams.remove(containerId);
            }
        });
        activeStreams.put(containerId, stream);
    }

    private void dispararAlertaProactiva(String containerId, long usageMB) {
        // Solo avisar una vez cada 30 minutos por contenedor para no hacer spam
        LocalDateTime ultimaAlerta = alertasEnviadas.get(containerId);
        if (ultimaAlerta == null || ultimaAlerta.isBefore(LocalDateTime.now().minusMinutes(30))) {

            String nombre = "Contenedor " + containerId.substring(0, 8); // Simplificado

            sentinelEventService.registrarEvento(
                    nombre, containerId, "WARNING",
                    "Consumo de RAM elevado: " + usageMB + "MB"
            );

            slackService.enviarNotificacion("⚠️ *AVISO PREVENTIVO - SENTINEL*\n" +
                    "El contenedor `" + nombre + "` está consumiendo mucha memoria: *" + usageMB + "MB*.\n" +
                    "Por favor, revisa posibles fugas de memoria (Memory Leaks).");

            alertasEnviadas.put(containerId, LocalDateTime.now());
        }
    }

    private String calcularCpu(Statistics stats) {
        try {
            if (stats.getCpuStats() == null || stats.getPreCpuStats() == null) return "0.00%";

            // Uso total de CPU del contenedor
            long cpuDelta = stats.getCpuStats().getCpuUsage().getTotalUsage() -
                    stats.getPreCpuStats().getCpuUsage().getTotalUsage();

            // Uso total del sistema
            long systemDelta = stats.getCpuStats().getSystemCpuUsage() -
                    stats.getPreCpuStats().getSystemCpuUsage();

            // Número de núcleos (vital para el cálculo correcto)
            Long onlineCpus = stats.getCpuStats().getOnlineCpus();
            if (onlineCpus == null) onlineCpus = 1L;

            if (systemDelta > 0 && cpuDelta > 0) {
                double cpuPct = ((double) cpuDelta / systemDelta) * onlineCpus * 100.0;
                return String.format("%.2f%%", cpuPct);
            }
        } catch (Exception e) {
            System.err.println("Error calculando CPU: " + e.getMessage());
        }
        return "0.00%";
    }

    private String calcularMemoria(Statistics stats) {
        try {
            if (stats.getMemoryStats() == null || stats.getMemoryStats().getUsage() == null) return "0MB";
            long usage = stats.getMemoryStats().getUsage() / (1024 * 1024);
            return usage + "MB";
        } catch (Exception e) {
            System.err.println("Error calculando Memoria: " + e.getMessage());
        }
        return "0MB";
    }

    private void procesarFalla(String containerId, String nombre) {
        int intentosActuales = retryTracker.getOrDefault(nombre, 0);

        if (intentosActuales < MAX_RETRIES) {
            retryTracker.put(nombre, intentosActuales + 1);

            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "FAILURE",
                    "Intento de recuperación #" + (intentosActuales + 1) + " de " + MAX_RETRIES
            );

            revivirContenedor(containerId, nombre);

        } else if (intentosActuales == MAX_RETRIES) {
            // Se alcanzó el límite: Marcamos como bloqueado y notificamos una única vez
            retryTracker.put(nombre, MAX_RETRIES + 1);

            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "CRITICAL_ERROR",
                    "ANTI-LOOP: Límite de reintentos alcanzado. Se suspende recuperación automática."
            );

            slackService.enviarNotificacion("🚨 *ALERTA CRÍTICA - ANTI-LOOP*\n" +
                    "El contenedor `" + nombre + "` falló repetidamente y ha sido bloqueado.\n" +
                    "Sentinel dejará de intentar reiniciarlo hasta que sea revisado manualmente.");
        }
    }

    private void revivirContenedor(String containerId, String nombre) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();

            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "RECOVERY",
                    "Sentinel restauró el servicio exitosamente."
            );

            String alerta = "*Sentinel Self-Healing Report*\n" +
                    "Contenedor restaurado: `" + nombre + "`\n" +
                    "Estado: Recuperado. Reintentos realizados: " + retryTracker.get(nombre);
            slackService.enviarNotificacion(alerta);

        } catch (Exception e) {
            sentinelEventService.registrarEvento(
                    nombre,
                    containerId,
                    "CRITICAL_ERROR",
                    "Error al ejecutar restart: " + e.getMessage()
            );
        }
    }
}
