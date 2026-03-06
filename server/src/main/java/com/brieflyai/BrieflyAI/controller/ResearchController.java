package com.brieflyai.BrieflyAI.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.service.ResearchService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @PostMapping("/process")
    public ResponseEntity<String> processContent(
            @Valid @RequestBody ResearchRequest researchRequest) {
        String result = researchService.processContent(researchRequest);
        return ResponseEntity.ok(result);
    }
}
