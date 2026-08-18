package com.example.dreamjournal.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class DateParser {

    private DateParser() {
    }

    static LocalDate parse(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date must use yyyy-MM-dd format");
        }
    }
}
