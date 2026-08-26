package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.WeeklyInsightResponse;
import com.example.dreamjournal.service.WeeklyInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userId}/insights")
public class WeeklyInsightController {

    private final WeeklyInsightService weeklyInsightService;

    public WeeklyInsightController(
            WeeklyInsightService weeklyInsightService
    ) {
        this.weeklyInsightService = weeklyInsightService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyInsightResponse> getWeeklyInsights(
            @PathVariable String userId,
            @RequestParam LocalDate startDate
    ) {
        return ResponseEntity.ok(
                weeklyInsightService.getWeeklyInsights(
                        userId,
                        startDate
                )
        );
    }

    @PostMapping("/weekly/refresh")
    public ResponseEntity<WeeklyInsightResponse> refreshWeeklyInsights(
            @PathVariable String userId,
            @RequestParam LocalDate startDate
    ) {
        return ResponseEntity.ok(
                weeklyInsightService.refreshWeeklyInsights(
                        userId,
                        startDate
                )
        );
    }
}