
package com.tbits.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("application", "tBits-AI");
        response.put("version", "1.0");
        response.put("server", "Spring Boot");

        return response;
    }
}

