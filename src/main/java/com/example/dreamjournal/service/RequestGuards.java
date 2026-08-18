package com.example.dreamjournal.service;

final class RequestGuards {

    private RequestGuards() {
    }

    static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }

    static void requireDreamId(String dreamId) {
        if (dreamId == null || dreamId.isBlank()) {
            throw new IllegalArgumentException("dreamId must not be blank");
        }
    }
}
