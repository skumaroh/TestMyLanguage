package com.infotech.lang.application.ai;

import java.util.Map;

/**
 * @param purpose      short label for the call log, for example PROCTOR_TEXT
 * @param systemPrompt the standing instructions
 * @param userPrompt   the task
 * @param jsonSchema   JSON Schema (as a map) the answer must follow
 * @param imagePng     an optional picture sent with the task (for example one handwritten character); null for text only
 * @param timeoutSeconds how long to wait for this call; null for the gateway's default (long answers need more)
 */
public record AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema,
                        byte[] imagePng, Integer timeoutSeconds) {

    /** A request with an optional picture and the default timeout. */
    public AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema, byte[] imagePng) {
        this(purpose, systemPrompt, userPrompt, jsonSchema, imagePng, null);
    }

    /** A text-only request. */
    public AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        this(purpose, systemPrompt, userPrompt, jsonSchema, null, null);
    }

    /** A text-only request that may take longer than the default. */
    public AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema, int timeoutSeconds) {
        this(purpose, systemPrompt, userPrompt, jsonSchema, null, timeoutSeconds);
    }
}
