package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.DreamResponse;
import com.example.dreamjournal.service.DreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/dreams")
public class DreamTagsController {

    private final DreamService dreamService;

    public DreamTagsController(DreamService dreamService) {
        this.dreamService = dreamService;
    }

    @GetMapping("/tags/recent")
    @Operation(summary = "Get recent dream tags")
    @ApiResponse(responseCode = "200", description = "Recent tags returned")
    public List<String> recentTags(
            @PathVariable String userId
    ) {
        return dreamService.findRecentTags(userId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search dreams")
    @ApiResponse(responseCode = "200", description = "Matching dreams returned")
    public List<DreamResponse> search(
            @PathVariable String userId,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String dreamType,
            @RequestParam(required = false) String tag
    ) {
        return dreamService.search(
                        userId,
                        text,
                        mood,
                        dreamType,
                        tag
                )
                .stream()
                .map(DreamResponse::from)
                .toList();
    }
}