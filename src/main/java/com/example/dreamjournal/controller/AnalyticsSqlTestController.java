package com.example.dreamjournal.controller;

import com.example.dreamjournal.security.FirebaseUser;
import com.example.dreamjournal.service.AnalyticsSqlService;
import com.example.dreamjournal.service.AnalyticsSqlValidator;
import com.example.dreamjournal.service.UserAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics-test")
public class AnalyticsSqlTestController {

    private final AnalyticsSqlService analyticsSqlService;
    private final AnalyticsSqlValidator sqlValidator;
    private final UserAnalyticsService userAnalyticsService;

    public AnalyticsSqlTestController(
            AnalyticsSqlService analyticsSqlService,
            AnalyticsSqlValidator sqlValidator,
            UserAnalyticsService userAnalyticsService
    ) {
        this.analyticsSqlService = analyticsSqlService;
        this.sqlValidator = sqlValidator;
        this.userAnalyticsService = userAnalyticsService;
    }

    @GetMapping("/sql")
    public ResponseEntity<?> generateAndExecute(
            @RequestParam String question,
            HttpServletRequest request
    ) {
        String uid = FirebaseUser.getUid(request);

        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        try {
            String sql =
                    analyticsSqlService.generateSql(question);

            sqlValidator.validate(sql);

            var results =
                    userAnalyticsService.executeUserQuery(
                            sql,
                            uid
                    );

            String answer =
                    analyticsSqlService.interpretResults(
                            question,
                            results
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "answer", answer
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", e.getMessage()
                    )
            );
        }
    }
}