package com.farias.sentinel.service;

import com.farias.sentinel.model.ContainerEvent;
import com.farias.sentinel.repository.SentinelEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SentinelEventService {

    @Autowired
    private SentinelEventRepository sentinelEventRepository;

    public void registrarEvento(String nombre, String id, String tipo, String detalle) {
        ContainerEvent evento = new ContainerEvent();
        evento.setContainerName(nombre);
        evento.setContainerId(id);
        evento.setEventType(tipo);
        evento.setDetails(detalle);
        evento.setTimestamp(LocalDateTime.now());

        sentinelEventRepository.save(evento);
    }

    public List<ContainerEvent> getRecentEvents() {
        // obtengo los ultimos 10 eventos ordenados por fecha descendente
        return sentinelEventRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();
    }
}
