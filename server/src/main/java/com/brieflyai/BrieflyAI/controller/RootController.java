package com.brieflyai.BrieflyAI.controller;

import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "service", "BrieflyAI",
                "status", "running",
                "timestamp", LocalDateTime.now().toString());
    }
}
