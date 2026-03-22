package com.farias.sentinel.controller;

import com.farias.sentinel.model.ContainerEvent;
import com.farias.sentinel.service.AnalyticsService;
import com.farias.sentinel.service.SentinelEventService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class SentinelEventController {

    @Autowired
    private SentinelEventService sentinelEventService;

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/recent")
    public List<ContainerEvent> getRecentEvents() {
        return sentinelEventService.getRecentEvents();
    }

    // Estadísticas globales para las tarjetas del Dashboard
    @GetMapping("/stats")
    public Map<String, Long> getGlobalStats() {
        return analyticsService.getGlobalStats();
    }

    // Ranking de contenedores problemáticos
    @GetMapping("/top-offenders")
    public List<Map<String, Object>> getTopOffenders() {
        return analyticsService.getTopOffenders();
    }

    // Exportación a CSV
    @GetMapping("/export")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=sentinel_history.csv");
        analyticsService.exportEventsToCSV(response.getWriter());
    }
}
