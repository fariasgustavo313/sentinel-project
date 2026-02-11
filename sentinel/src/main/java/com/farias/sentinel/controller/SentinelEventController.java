package com.farias.sentinel.controller;

import com.farias.sentinel.model.ContainerEvent;
import com.farias.sentinel.service.SentinelEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class SentinelEventController {

    @Autowired
    private SentinelEventService sentinelEventService;

    @GetMapping("/recent")
    public List<ContainerEvent> getRecentEvents() {
        return sentinelEventService.getRecentEvents();
    }
}
