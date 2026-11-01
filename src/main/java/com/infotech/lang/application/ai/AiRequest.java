package com.infotech.lang.application.ai;

import java.util.Map;

/**
 * @param purpose      short label for the call log, for example PROCTOR_TEXT
 * @param systemPrompt the standing instructions
 * @param userPrompt   the task
 * @param jsonSchema   JSON Schema (as a map) the answer must follow
 * @param imagePng     an optional picture sent with the task (for example one handwritten character); null for text only
 */
public record AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema,
                        byte[] imagePng) {

    /** A text-only request. */
    public AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        this(purpose, systemPrompt, userPrompt, jsonSchema, null);
    }
}
