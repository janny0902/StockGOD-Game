package com.jadiss.cldc.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${app.admin.username}")
    private String adminUser;

    // Business logic: Provide quick runtime status used by container orchestration and smoke tests.
    @GetMapping
    public Map<String, String> health() {
        return Map.of(
                "service", "chart-learning-data-collector-backend",
                "status", "UP",
                "adminUser", adminUser
        );
    }
}
