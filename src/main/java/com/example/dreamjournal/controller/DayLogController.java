package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.DayDetailsResponse;
import com.example.dreamjournal.dto.DayLogRequest;
import com.example.dreamjournal.dto.DayLogResponse;
import com.example.dreamjournal.service.DayLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/days")
@Tag(name = "Day Logs")
public class DayLogController {

    private final DayLogService dayLogService;

    public DayLogController(DayLogService dayLogService) {
        this.dayLogService = dayLogService;
    }

    @PutMapping("/{date}")
    @Operation(summary = "Create or update a day log")
    @ApiResponse(responseCode = "200", description = "Day log saved")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public DayLogResponse upsert(
            @PathVariable String userId,
            @PathVariable String date,
            @Valid @RequestBody DayLogRequest request
    ) {
        return DayLogResponse.from(dayLogService.upsert(userId, date, request));
    }

    @GetMapping("/{date}")
    @Operation(summary = "Get a day log")
    @ApiResponse(responseCode = "200", description = "Day log found")
    @ApiResponse(responseCode = "404", description = "Day log not found")
    public DayLogResponse get(@PathVariable String userId, @PathVariable String date) {
        return DayLogResponse.from(dayLogService.get(userId, date));
    }

    @GetMapping
    @Operation(summary = "List day logs for a user")
    public List<DayLogResponse> list(
            @PathVariable String userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return dayLogService.list(userId, from, to).stream()
                .map(DayLogResponse::from)
                .toList();
    }

    @DeleteMapping("/{date}")
    @Operation(summary = "Delete a day log and all dreams for that day")
    @ApiResponse(responseCode = "204", description = "Day deleted")
    @ApiResponse(responseCode = "404", description = "Day log not found")
    public ResponseEntity<Void> delete(@PathVariable String userId, @PathVariable String date) {
        dayLogService.delete(userId, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{date}/details")
    @Operation(summary = "Get combined day sleep and dream details")
    @ApiResponse(responseCode = "200", description = "Day details found")
    @ApiResponse(responseCode = "404", description = "No day log or dreams found")
    public DayDetailsResponse details(@PathVariable String userId, @PathVariable String date) {
        return dayLogService.details(userId, date);
    }
}
