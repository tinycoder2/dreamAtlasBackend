package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.DreamReorderRequest;
import com.example.dreamjournal.dto.DreamRequest;
import com.example.dreamjournal.dto.DreamResponse;
import com.example.dreamjournal.service.DreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/days/{date}/dreams")
@Tag(name = "Dreams")
public class DreamController {

    private final DreamService dreamService;

    public DreamController(DreamService dreamService) {
        this.dreamService = dreamService;
    }

    @PostMapping
    @Operation(summary = "Create a dream")
    @ApiResponse(responseCode = "201", description = "Dream created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<DreamResponse> create(
            @PathVariable String userId,
            @PathVariable String date,
            @Valid @RequestBody DreamRequest request
    ) {
        DreamResponse response = DreamResponse.from(dreamService.create(userId, date, request));
        URI location = URI.create("/api/users/%s/days/%s/dreams/%s".formatted(userId, date, response.id()));
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/order")
    @Operation(summary = "Reorder dreams for a date")
    @ApiResponse(responseCode = "200", description = "Dreams reordered")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public List<DreamResponse> reorder(
            @PathVariable String userId,
            @PathVariable String date,
            @Valid @RequestBody DreamReorderRequest request
    ) {
        return dreamService.reorder(
                        userId,
                        date,
                        request.orderedIds()
                )
                .stream()
                .map(DreamResponse::from)
                .toList();
    }

    @GetMapping("/{dreamId}")
    @Operation(summary = "Get a dream")
    @ApiResponse(responseCode = "200", description = "Dream found")
    @ApiResponse(responseCode = "404", description = "Dream not found")
    public DreamResponse get(
            @PathVariable String userId,
            @PathVariable String date,
            @PathVariable String dreamId
    ) {
        return DreamResponse.from(dreamService.get(userId, date, dreamId));
    }

    @PutMapping("/{dreamId}")
    @Operation(summary = "Update a dream")
    @ApiResponse(responseCode = "200", description = "Dream updated")
    @ApiResponse(responseCode = "404", description = "Dream not found")
    public DreamResponse update(
            @PathVariable String userId,
            @PathVariable String date,
            @PathVariable String dreamId,
            @Valid @RequestBody DreamRequest request
    ) {
        return DreamResponse.from(dreamService.update(userId, date, dreamId, request));
    }

    @DeleteMapping("/{dreamId}")
    @Operation(summary = "Delete a dream")
    @ApiResponse(responseCode = "204", description = "Dream deleted")
    @ApiResponse(responseCode = "404", description = "Dream not found")
    public ResponseEntity<Void> delete(
            @PathVariable String userId,
            @PathVariable String date,
            @PathVariable String dreamId
    ) {
        dreamService.delete(userId, date, dreamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List dreams for a date")
    public List<DreamResponse> list(@PathVariable String userId, @PathVariable String date) {
        return dreamService.list(userId, date).stream()
                .map(DreamResponse::from)
                .toList();
    }

    @GetMapping("/tags/recent")
    @Operation(summary = "Get recent dream tags")
    @ApiResponse(responseCode = "200", description = "Recent tags returned")
    public List<String> recentTags(
            @PathVariable String userId
    ) {
        return dreamService.findRecentTags(userId);
    }
}
