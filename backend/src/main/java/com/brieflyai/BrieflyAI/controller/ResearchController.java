package com.brieflyai.BrieflyAI.controller;

import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.service.ResearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResearchController {
    
    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) { 
        this.researchService = researchService;
    }

    @PostMapping("/process")
    public ResponseEntity<String> processContent(@RequestBody ResearchRequest researchRequest) {
        String result = researchService.processContent(researchRequest);

        return ResponseEntity.ok(result);
    }
}
