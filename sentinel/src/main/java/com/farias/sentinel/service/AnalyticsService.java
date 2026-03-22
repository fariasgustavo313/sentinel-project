package com.farias.sentinel.service;

import com.farias.sentinel.model.ContainerEvent;
import com.farias.sentinel.repository.SentinelEventRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final SentinelEventRepository repository;

    public AnalyticsService(SentinelEventRepository repository) {
        this.repository = repository;
    }

    public Map<String, Long> getGlobalStats() {
        List<Object[]> results = repository.countEventsByType();
        return results.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
        ));
    }

    public List<Map<String, Object>> getTopOffenders() {
        return repository.findTopContainerFailures().stream().limit(5).map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("count", row[1]);
            return map;
        }).collect(Collectors.toList());
    }

    public void exportEventsToCSV(java.io.PrintWriter writer) {
        writer.println("Timestamp,Container,Event,Details");
        List<ContainerEvent> events = repository.findAll(); // O usa un orden específico
        for (ContainerEvent event : events) {
            writer.printf("%s,%s,%s,%s%n",
                    event.getTimestamp(),
                    event.getContainerName(),
                    event.getEventType(),
                    event.getDetails());
        }
    }
}
