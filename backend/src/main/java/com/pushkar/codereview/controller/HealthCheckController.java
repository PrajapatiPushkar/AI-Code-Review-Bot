package com.pushkar.codereview.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "AI Code Review Bot Backend",
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(status);
    }
}
