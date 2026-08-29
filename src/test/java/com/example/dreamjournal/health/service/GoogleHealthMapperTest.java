package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class GoogleHealthMapperTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final GoogleHealthMapper mapper =
            new GoogleHealthMapper();

    @Test
    void mapsSleepFromGoogleHealthResponse() throws Exception {

        String json = """
            {
              "dataPoints": [
                {
                  "name": "users/7583570207748835967/dataTypes/sleep/dataPoints/1218857650621109392",
                  "dataSource": {
                    "recordingMethod": "MANUAL",
                    "platform": "FITBIT"
                  },
                  "sleep": {
                    "interval": {
                      "startTime": "2026-08-27T17:24:00Z",
                      "startUtcOffset": "19800s",
                      "endTime": "2026-08-28T01:24:00Z",
                      "endUtcOffset": "19800s"
                    },
                    "type": "CLASSIC",
                    "stages": [
                      {
                        "startTime": "2026-08-27T17:24:00Z",
                        "startUtcOffset": "19800s",
                        "endTime": "2026-08-28T01:24:00Z",
                        "endUtcOffset": "19800s",
                        "type": "ASLEEP"
                      }
                    ],
                    "metadata": {
                      "processed": true,
                      "manuallyEdited": true,
                      "mainSleep": true
                    },
                    "summary": {
                      "minutesInSleepPeriod": "480",
                      "minutesAfterWakeUp": "0",
                      "minutesToFallAsleep": "0",
                      "minutesAsleep": "480",
                      "minutesAwake": "0"
                    }
                  }
                }
              ]
            }
            """;

        JsonNode root =
                objectMapper.readTree(json);

        SleepSession sleep =
                mapper.mapSleep(
                        root.path("dataPoints").get(0)
                );

        assertEquals(
                "2026-08-27T17:24:00Z",
                sleep.startTimeUtc().toString()
        );

        assertEquals(
                "2026-08-28T01:24:00Z",
                sleep.endTimeUtc().toString()
        );

        assertEquals(
                ZoneOffset.ofHoursMinutes(5, 30),
                sleep.startOffset()
        );

        assertEquals(
                ZoneOffset.ofHoursMinutes(5, 30),
                sleep.endOffset()
        );

        assertEquals(
                "CLASSIC",
                sleep.type()
        );

        assertTrue(sleep.mainSleep());

        assertEquals(
                "FITBIT",
                sleep.platform()
        );

        assertEquals(
                "MANUAL",
                sleep.recordingMethod()
        );

        assertEquals(
                480,
                sleep.durationMinutes()
        );

        assertEquals(
                480,
                sleep.minutesAsleep()
        );

        assertEquals(
                0,
                sleep.minutesAwake()
        );

        assertEquals(
                1,
                sleep.stages().size()
        );

        assertEquals(
                "ASLEEP",
                sleep.stages().get(0).type()
        );

        assertEquals(
                480,
                sleep.stages().get(0).durationMinutes()
        );
    }


    @Test
    void mapsHeartRateFromGoogleHealthResponse() throws Exception {

        String json = """
                {
                    "dataPoints": [
                        {
                            "dataSource": {
                                "recordingMethod": "PASSIVELY_MEASURED",
                                "device": {
                                    "displayName": "Inspire 3"
                                },
                                "platform": "FITBIT"
                            },
                            "heartRate": {
                                "sampleTime": {
                                    "physicalTime": "2026-08-29T05:29:57Z",
                                    "utcOffset": "19800s",
                                    "civilTime": {
                                        "date": {
                                            "year": 2026,
                                            "month": 8,
                                            "day": 29
                                        },
                                        "time": {
                                            "hours": 10,
                                            "minutes": 59,
                                            "seconds": 57
                                        }
                                    }
                                },
                                "beatsPerMinute": "61"
                            }
                        }
                    ]
                }
            """;

        JsonNode root =
                objectMapper.readTree(json);

        HeartRateSample sample =
                mapper.mapHeartRate(
                        root.path("dataPoints").get(0)
                );

        assertEquals(
                "2026-08-29T05:29:57Z",
                sample.timestampUtc().toString()
        );

        assertEquals(
                ZoneOffset.ofHoursMinutes(5, 30),
                sample.utcOffset()
        );

        assertEquals(
                61,
                sample.beatsPerMinute()
        );

        assertEquals(
                "FITBIT",
                sample.platform()
        );

        assertEquals(
                "Inspire 3",
                sample.deviceName()
        );

        assertEquals(
                "PASSIVELY_MEASURED",
                sample.recordingMethod()
        );
    }
}