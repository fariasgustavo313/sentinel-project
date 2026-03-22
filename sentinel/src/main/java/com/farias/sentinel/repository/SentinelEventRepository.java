package com.farias.sentinel.repository;

import com.farias.sentinel.model.ContainerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentinelEventRepository extends JpaRepository<ContainerEvent, Long> {

    // Obtener los últimos 10 eventos (ya lo deberías tener)
    List<ContainerEvent> findTop10ByOrderByTimestampDesc();

    // Contar eventos por tipo (FAILURE, RECOVERY, CRITICAL_ERROR, WARNING)
    @Query("SELECT e.eventType, COUNT(e) FROM ContainerEvent e GROUP BY e.eventType")
    List<Object[]> countEventsByType();

    // Ranking de los 5 contenedores con más fallas
    @Query("SELECT e.containerName, COUNT(e) FROM ContainerEvent e WHERE e.eventType = 'FAILURE' OR e.eventType = 'CRITICAL_ERROR' GROUP BY e.containerName ORDER BY COUNT(e) DESC")
    List<Object[]> findTopContainerFailures();
}
