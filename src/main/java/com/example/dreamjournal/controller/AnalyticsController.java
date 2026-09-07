package com.example.dreamjournal.controller;

import com.example.dreamjournal.dto.AnalyticsChatRequest;
import com.example.dreamjournal.security.FirebaseUser;
import com.example.dreamjournal.service.AnalyticsSqlService;
import com.example.dreamjournal.service.AnalyticsSqlValidator;
import com.example.dreamjournal.service.UserAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsSqlService analyticsSqlService;
    private final AnalyticsSqlValidator analyticsSqlValidator;
    private final UserAnalyticsService userAnalyticsService;

    public AnalyticsController(
            AnalyticsSqlService analyticsSqlService,
            AnalyticsSqlValidator analyticsSqlValidator,
            UserAnalyticsService userAnalyticsService
    ) {
        this.analyticsSqlService = analyticsSqlService;
        this.analyticsSqlValidator = analyticsSqlValidator;
        this.userAnalyticsService = userAnalyticsService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @Valid @RequestBody AnalyticsChatRequest request,
            HttpServletRequest httpRequest
    ) {

        /*
         * IMPORTANT:
         *
         * This UID comes exclusively from the verified
         * Firebase ID token.
         *
         * Never take userId from the request body.
         */
        String firebaseUid =
                FirebaseUser.getUid(httpRequest);

        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "error",
                            "Unauthorized"
                    ));
        }

        try {

            /*
             * 1. Gemini converts the natural-language question
             *    into analytical SQL.
             */
            String sql =
                    analyticsSqlService.generateSql(
                            request.message()
                    );

            /*
             * 2. Validate Gemini's SQL before execution.
             */
            analyticsSqlValidator.validate(sql);

            /*
             * 3. Execute ONLY against this authenticated
             *    user's data.
             */
            var results =
                    userAnalyticsService.executeUserQuery(
                            sql,
                            firebaseUid
                    );

            /*
             * 4. Gemini interprets the user-scoped results.
             */
            String answer =
                    analyticsSqlService.interpretResults(
                            request.message(),
                            results
                    );

            /*
             * 5. Return ONLY the natural-language answer.
             *
             * Do not expose:
             * - SQL
             * - Firebase UID
             * - raw BigQuery results
             */
            return ResponseEntity.ok(
                    Map.of(
                            "answer",
                            answer
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "error",
                            "Unable to process analytics request"
                    ));
        }
    }
}