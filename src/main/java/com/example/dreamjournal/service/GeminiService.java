package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.GeminiDreamResponse;
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