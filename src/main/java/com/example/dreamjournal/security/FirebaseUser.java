package com.example.dreamjournal.security;

import jakarta.servlet.http.HttpServletRequest;

public final class FirebaseUser {

    private FirebaseUser() {
    }

    public static String getUid(HttpServletRequest request) {
        return (String) request.getAttribute("firebaseUid");
    }
}