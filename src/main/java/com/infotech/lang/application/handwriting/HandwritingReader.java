package com.infotech.lang.application.handwriting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiRequest;
import com.infotech.lang.application.ai.AiResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Asks the Meta vision model which single character a drawing shows and how sure it is. The model reads the drawing
 * blind (it is not told which letter was expected); our own code compares the answer with the expected letter and applies
 * the thresholds, so the model never decides correctness.
 */
@Service
public class HandwritingReader {

    public static final int PROMPT_VERSION = 1;

    static final String SYSTEM = "You read one handwritten character from a picture and answer only with the requested JSON.";

    private final AiGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    public HandwritingReader(AiGateway gateway) {
        this.gateway = gateway;
    }

    /** What the model read. {@code ok} is false when the call failed or the answer could not be used. */
    public record Reading(boolean ok, String character, double confidence, long latencyMs, String error) {
        static Reading failed(long latencyMs, String error) {
            return new Reading(false, "", 0, latencyMs, error);
        }
    }

    /** {@code language} is {@code tamil} or {@code hindi}. Never throws. */
    public Reading read(byte[] drawingPng, String language) {
        AiResult result = gateway.completeJson(new AiRequest("HANDWRITING_READ", SYSTEM, task(language), schema(), drawingPng));
        if (!result.ok()) {
            return Reading.failed(result.latencyMs(), result.error());
        }
        return parse(result.content(), result.latencyMs());
    }

    Reading parse(String json, long latencyMs) {
        try {
            JsonNode root = mapper.readTree(json);
            String character = root.path("character").asText("").trim();
            double confidence = root.path("confidence").asDouble(0);
            if (Double.isNaN(confidence)) {
                confidence = 0;
            }
            confidence = Math.max(0, Math.min(1, confidence));
            return new Reading(true, character, confidence, latencyMs, null);
        } catch (Exception e) {
            return Reading.failed(latencyMs, "unreadable answer");
        }
    }

    static String task(String language) {
        String script = "hindi".equalsIgnoreCase(language) ? "Hindi (Devanagari script)" : "Tamil script";
        return "The picture shows one handwritten character in " + script + ", drawn by a learner with a finger or mouse "
                + "on a plain background. Say which character it is and how confident you are, from 0 (not sure at all) "
                + "to 1 (certain). Read what is drawn, not what the learner may have meant. If you cannot read a "
                + "character, return an empty string and confidence 0.";
    }

    static Map<String, Object> schema() {
        Map<String, Object> character = new LinkedHashMap<>();
        character.put("type", "string");
        Map<String, Object> confidence = new LinkedHashMap<>();
        confidence.put("type", "number");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("character", character);
        properties.put("confidence", confidence);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("character", "confidence"));
        schema.put("additionalProperties", false);
        return schema;
    }
}
