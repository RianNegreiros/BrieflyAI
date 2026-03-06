package com.brieflyai.BrieflyAI.controller;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("service", "BrieflyAI", "status", "running", "timestamp",
                LocalDateTime.now().toString());
    }
}
