package com.example.dreamjournal.controller;

import com.example.dreamjournal.security.FirebaseUser;
import com.example.dreamjournal.service.UserAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics-test")
public class UserAnalyticsTestController {

    private final UserAnalyticsService userAnalyticsService;

    public UserAnalyticsTestController(
            UserAnalyticsService userAnalyticsService
    ) {
        this.userAnalyticsService = userAnalyticsService;
    }

    @GetMapping("/user-data")
    public ResponseEntity<?> getUserData(
            HttpServletRequest request
    ) {
        String uid = FirebaseUser.getUid(request);

        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(
                userAnalyticsService.getDailyDreamSleep(uid)
        );
    }
}