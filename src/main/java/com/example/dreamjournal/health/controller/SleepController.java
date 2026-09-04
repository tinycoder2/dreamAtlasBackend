package com.example.dreamjournal.health.controller;

import com.example.dreamjournal.health.model.WeeklySleepResponse;
import com.example.dreamjournal.health.service.SleepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userId}/sleep")
public class SleepController {

    private final SleepService sleepService;

    public SleepController(SleepService sleepService) {
        this.sleepService = sleepService;
    }

    @GetMapping
    public ResponseEntity<WeeklySleepResponse> getWeeklySleep(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @PathVariable String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        if (!firebaseUid.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        try {

            WeeklySleepResponse response =
                    sleepService.getWeeklySleep(
                            userId,
                            startDate,
                            endDate
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }
}