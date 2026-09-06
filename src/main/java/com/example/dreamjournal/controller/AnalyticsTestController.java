package com.example.dreamjournal.controller;

import com.example.dreamjournal.service.BigQueryAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/analytics-test")
public class AnalyticsTestController {

    private final BigQueryAgentService bigQueryAgentService;

    public AnalyticsTestController(
            BigQueryAgentService bigQueryAgentService
    ) {
        this.bigQueryAgentService = bigQueryAgentService;
    }
}