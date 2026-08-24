package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.GeminiDreamResponse;
import com.example.dreamjournal.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/test")
public class GeminiController {
    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/audio")
    public ResponseEntity<GeminiDreamResponse> uploadAudio(
            @RequestParam("audio") MultipartFile audio
    ) throws IOException {


        GeminiDreamResponse result =
                geminiService.extractDreamsFromAudio(audio.getBytes());

        return ResponseEntity.ok(result);
    }
}