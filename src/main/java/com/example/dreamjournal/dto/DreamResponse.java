package com.example.dreamjournal.dto;

import com.example.dreamjournal.model.Dream;

import java.time.Instant;
import java.util.List;

public record DreamResponse(
        String id,
        String date,
        String text,
        String mood,
        String dreamType,
        List<String> tags,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static DreamResponse from(Dream dream) {
        return new DreamResponse(
                dream.id(),
                dream.date().toString(),
                dream.text(),
                dream.mood(),
                dream.dreamType(),
                dream.tags() == null ? List.of() : dream.tags(),
                dream.sortOrder(),
                dream.createdAt(),
                dream.updatedAt()
        );
    }
}
