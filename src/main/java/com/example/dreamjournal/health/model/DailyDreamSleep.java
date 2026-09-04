package com.example.dreamjournal.health.model;

import java.time.LocalDate;

public record DailyDreamSleep(
        LocalDate journalDate,
        int dreamCount,
        int greatDreams,
        int goodDreams,
        int neutralDreams,
        int badDreams,
        int nightmares,
        int lucidDreams,
        int vividDreams,
        int recurringDreams,
        Integer minutesAsleep,
        Integer minutesAwake,
        Integer deepMinutes,
        Integer lightMinutes,
        Integer remMinutes,
        Double meanHr,
        Integer minHr,
        Integer maxHr,
        Double hrStddev
) {}