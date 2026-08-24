package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.DreamReorderRequest;
import com.example.dreamjournal.dto.DreamRequest;
import com.example.dreamjournal.dto.DreamResponse;
import com.example.dreamjournal.dto.GeminiDreamResponse;
import com.example.dreamjournal.security.FirebaseUser;
import com.example.dreamjournal.service.DreamService;
import com.example.dreamjournal.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/days/{date}/dreams")
@Tag(name = "Dreams")
public class DreamController {

    private final DreamService dreamService;
    private final GeminiService geminiService;

    public DreamController(
            DreamService dreamService,
            GeminiService geminiService
    ) {
        this.dreamService = dreamService;
        this.geminiService = geminiService;
    }

    @PostMapping("/ai")
    public ResponseEntity<List<DreamResponse>> createFromAudio(
            @PathVariable String date,
            @RequestParam("audio") MultipartFile audio,
            HttpServletRequest httpRequest
    ) throws IOException {

        String userId = FirebaseUser.getUid(httpRequest);

        GeminiDreamResponse extracted =
                geminiService.extractDreamsFromAudio(audio.getBytes());

        List<DreamResponse> dreams =
                dreamService.createFromGemini(
                                userId,
                                date,
                                extracted
                        )
                        .stream()
                        .map(DreamResponse::from)
                        .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(dreams);
    }

    @PostMapping
    @Operation(summary = "Create a dream")
    @ApiResponse(responseCode = "201", description = "Dream created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<DreamResponse> create(
            @PathVariable String date,
            @Valid @RequestBody DreamRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        DreamResponse response =
                DreamResponse.from(dreamService.create(userId, date, request));

        URI location = URI.create(
                "/api/users/%s/days/%s/dreams/%s"
                        .formatted(userId, date, response.id())
        );

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/order")
    @Operation(summary = "Reorder dreams for a date")
    @ApiResponse(responseCode = "200", description = "Dreams reordered")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public List<DreamResponse> reorder(
            @PathVariable String date,
            @Valid @RequestBody DreamReorderRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

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
            @PathVariable String date,
            @PathVariable String dreamId,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        return DreamResponse.from(
                dreamService.get(userId, date, dreamId)
        );
    }

    @PutMapping("/{dreamId}")
    @Operation(summary = "Update a dream")
    @ApiResponse(responseCode = "200", description = "Dream updated")
    @ApiResponse(responseCode = "404", description = "Dream not found")
    public DreamResponse update(
            @PathVariable String date,
            @PathVariable String dreamId,
            @Valid @RequestBody DreamRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        return DreamResponse.from(
                dreamService.update(userId, date, dreamId, request)
        );
    }

    @DeleteMapping("/{dreamId}")
    @Operation(summary = "Delete a dream")
    @ApiResponse(responseCode = "204", description = "Dream deleted")
    @ApiResponse(responseCode = "404", description = "Dream not found")
    public ResponseEntity<Void> delete(
            @PathVariable String date,
            @PathVariable String dreamId,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        dreamService.delete(userId, date, dreamId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List dreams for a date")
    public List<DreamResponse> list(
            @PathVariable String date,
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        return dreamService.list(userId, date).stream()
                .map(DreamResponse::from)
                .toList();
    }

    @GetMapping("/tags/recent")
    @Operation(summary = "Get recent dream tags")
    @ApiResponse(responseCode = "200", description = "Recent tags returned")
    public List<String> recentTags(
            HttpServletRequest httpRequest
    ) {
        String userId = FirebaseUser.getUid(httpRequest);

        return dreamService.findRecentTags(userId);
    }


}