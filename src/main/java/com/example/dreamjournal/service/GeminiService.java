package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.GeminiDreamResponse;
import com.example.dreamjournal.dto.GeminiWeeklyInsightResponse;
import com.example.dreamjournal.model.Dream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final ObjectMapper objectMapper;
    private final Client client;
    private final Schema dreamSchema;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.client = Client.builder()
                .vertexAI(true)
                .build();

        this.dreamSchema = buildDreamSchema();
    }

    public GeminiDreamResponse testDreamExtraction() {

        String transcript = """
                I had two dreams last night.

                The first one was really nice. I was flying over my college
                and I could see the whole city below me. I felt really happy
                and free.

                Then I woke up, went back to sleep, and had another dream.
                I was walking through a really dark forest and I got lost.
                I was scared because I could hear something following me.
                """;

        String prompt = """
                You are a dream journal assistant.

                The user has spoken a free-form rant about their dreams.

                Your job is to:
                1. Identify each distinct dream.
                2. Keep the dreams in the order they were described.
                3. Rewrite speech/transcription artifacts into readable text.
                4. Do not invent events that the user did not describe.
                5. Assign the most appropriate mood.
                6. Assign the most appropriate dream type.
                7. Extract useful short tags.

                Allowed moods:
                great, good, neutral, bad, nightmare

                Allowed dream types:
                normal, lucid, nightmare, recurring, vivid

                Return one dream for each distinct dream described.

                User transcript:

                """ + transcript;

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.0F)
                .responseMimeType("application/json")
                .responseSchema(dreamSchema)
                .build();

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        config
                );

        return parseResponse(response);
    }

    public GeminiDreamResponse extractDreamsFromAudio(byte[] audioBytes) {

        Part audioPart = Part.fromBytes(audioBytes, "audio/mp4");

        String prompt = """
                You are a dream journal assistant.

                Listen to the user's spoken dream journal entry.

                The user may freely rant about multiple dreams in one recording.

                Your job is to:
                1. Identify each distinct dream.
                2. Keep the dreams in the order they were described.
                3. Transcribe and clean up the user's speech into readable dream text.
                4. Do not invent events that the user did not describe.
                5. Assign the most appropriate mood.
                6. Assign the most appropriate dream type.
                7. Extract useful short tags.

                Allowed moods:
                great, good, neutral, bad, nightmare

                Allowed dream types:
                normal, lucid, nightmare, recurring, vivid

                Return one dream for each distinct dream described.
                """;

        Content content = Content.builder()
                .role("user")
                .parts(
                        Part.fromText(prompt),
                        audioPart
                )
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.0F)
                .responseMimeType("application/json")
                .responseSchema(dreamSchema)
                .build();

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        List.of(content),
                        config
                );

        return parseResponse(response);
    }

    private GeminiDreamResponse parseResponse(
            GenerateContentResponse response
    ) {
        try {
            return objectMapper.readValue(
                    response.text(),
                    GeminiDreamResponse.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse Gemini response",
                    e
            );
        }
    }

    public GeminiWeeklyInsightResponse generateWeeklyInsights(
            List<Dream> dreams
    ) {
        StringBuilder dreamsText = new StringBuilder();

        for (int i = 0; i < dreams.size(); i++) {
            Dream dream = dreams.get(i);

            dreamsText.append("""
                Dream %d
                Date: %s
                Text: %s

                """.formatted(
                    i + 1,
                    dream.date(),
                    dream.text()
            ));
        }

        String prompt = """
            You are a dream journal assistant generating weekly
            dream insights for personal reflection and self-exploration.

            Analyze ONLY the dreams provided below.

            Your job is to identify meaningful recurring patterns
            across the dreams.

            WEEKLY SUMMARY:
            - Write one short, concise summary of the overall themes
              and direction of the week's dreams.
            - Do not make psychological diagnoses.
            - Do not claim certainty about the user's psychological state.

            RECURRING THEMES:
            - Identify 1 to 5 meaningful recurring themes.
            - Only include themes supported by multiple dreams or
              strong evidence within the week's dreams.
            - Give each theme a prominence score from 0 to 100.
            - The prominence score represents how strongly the theme
              appears within this week's dreams.
            - It is NOT a probability or psychological measurement.
            - Use concise uppercase names such as CHANGE, CONTROL,
              RELATIONSHIPS, SEARCHING.

            EMOTIONAL PATTERNS:
            - Identify 0 to 3 meaningful recurring emotional patterns.
            - Do NOT invent emotional patterns just to reach three.
            - If there is insufficient evidence, return an empty array.
            - For each pattern, describe the recurring emotional
              experience.
            - Include a relevant Jungian-inspired concept only when
              genuinely appropriate.
            - The Jungian concept is optional and may be null.
            - Give a short, tentative interpretation.
            - Use language such as "may suggest", "could reflect",
              or "might point toward".
            - Do not diagnose or provide treatment.

            Do not invent events, emotions, themes, or patterns that
            are not reasonably supported by the dreams.

            Dreams from this week:

            """ + dreamsText;

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.2F)
                .responseMimeType("application/json")
                .responseSchema(weeklyInsightSchema())
                .build();

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        config
                );

        return parseWeeklyInsightResponse(response);
    }

    private GeminiWeeklyInsightResponse parseWeeklyInsightResponse(
            GenerateContentResponse response
    ) {
        try {
            return objectMapper.readValue(
                    response.text(),
                    GeminiWeeklyInsightResponse.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse Gemini weekly insight response",
                    e
            );
        }
    }

    private Schema weeklyInsightSchema() {
        Schema themeSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "name",
                        Schema.builder()
                                .type(Type.Known.STRING)
                                .build(),

                        "prominence",
                        Schema.builder()
                                .type(Type.Known.INTEGER)
                                .build()
                ))
                .required(List.of("name", "prominence"))
                .build();

        Schema emotionalPatternSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "pattern",
                        Schema.builder()
                                .type(Type.Known.STRING)
                                .build(),

                        "jungianConcept",
                        Schema.builder()
                                .type(Type.Known.STRING)
                                .build(),

                        "interpretation",
                        Schema.builder()
                                .type(Type.Known.STRING)
                                .build()
                ))
                .required(List.of(
                        "pattern",
                        "interpretation"
                ))
                .build();

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "weeklySummary",
                        Schema.builder()
                                .type(Type.Known.STRING)
                                .build(),

                        "themes",
                        Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(themeSchema)
                                .build(),

                        "emotionalPatterns",
                        Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(emotionalPatternSchema)
                                .build()
                ))
                .required(List.of(
                        "weeklySummary",
                        "themes",
                        "emotionalPatterns"
                ))
                .build();
    }

    private Schema buildDreamSchema() {

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "dreams",
                        Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(
                                        Schema.builder()
                                                .type(Type.Known.OBJECT)
                                                .properties(Map.of(

                                                        "text",
                                                        Schema.builder()
                                                                .type(Type.Known.STRING)
                                                                .build(),

                                                        "mood",
                                                        Schema.builder()
                                                                .type(Type.Known.STRING)
                                                                .enum_(List.of(
                                                                        "great",
                                                                        "good",
                                                                        "neutral",
                                                                        "bad",
                                                                        "nightmare"
                                                                ))
                                                                .build(),

                                                        "dreamType",
                                                        Schema.builder()
                                                                .type(Type.Known.STRING)
                                                                .enum_(List.of(
                                                                        "normal",
                                                                        "lucid",
                                                                        "nightmare",
                                                                        "recurring",
                                                                        "vivid"
                                                                ))
                                                                .build(),

                                                        "tags",
                                                        Schema.builder()
                                                                .type(Type.Known.ARRAY)
                                                                .items(
                                                                        Schema.builder()
                                                                                .type(Type.Known.STRING)
                                                                                .build()
                                                                )
                                                                .build()
                                                ))
                                                .required(List.of(
                                                        "text",
                                                        "mood",
                                                        "dreamType",
                                                        "tags"
                                                ))
                                                .build()
                                )
                                .build()
                ))
                .required(List.of("dreams"))
                .build();
    }
}