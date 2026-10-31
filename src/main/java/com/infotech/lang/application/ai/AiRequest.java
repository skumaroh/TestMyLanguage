package com.infotech.lang.application.ai;

import java.util.Map;

/**
 * @param purpose      short label for the call log, for example PROCTOR_TEXT
 * @param systemPrompt the standing instructions
 * @param userPrompt   the task
 * @param jsonSchema   JSON Schema (as a map) the answer must follow
 */
public record AiRequest(String purpose, String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
}
