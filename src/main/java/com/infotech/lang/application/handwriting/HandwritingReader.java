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

    /** Meta's answer to "which of these letters is it?": a confidence for each option it named (only options we offered). */
    public record Choice(boolean ok, Map<String, Double> confidences, long latencyMs, String error) {
        static Choice failed(long latencyMs, String error) {
            return new Choice(false, Map.of(), latencyMs, error);
        }

        /** The option with the highest confidence, or "" when there is none. */
        public String best() {
            return confidences.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        }
    }

    /** Asks the model to pick from {@code options} (a short list of letters, including the expected one). Never throws. */
    public Choice choose(byte[] drawingPng, String language, List<String> options) {
        AiResult result = gateway.completeJson(
                new AiRequest("HANDWRITING_CHOOSE", SYSTEM, choiceTask(language, options), choiceSchema(), drawingPng));
        if (!result.ok()) {
            return Choice.failed(result.latencyMs(), result.error());
        }
        return parseChoices(result.content(), options, result.latencyMs());
    }

    Choice parseChoices(String json, List<String> options, long latencyMs) {
        try {
            Map<String, Double> confidences = new LinkedHashMap<>();
            for (JsonNode item : mapper.readTree(json).path("choices")) {
                String character = item.path("character").asText("").trim();
                double confidence = Math.max(0, Math.min(1, item.path("confidence").asDouble(0)));
                if (options.contains(character)) {
                    confidences.merge(character, confidence, Math::max);
                }
            }
            return new Choice(true, confidences, latencyMs, null);
        } catch (Exception e) {
            return Choice.failed(latencyMs, "unreadable answer");
        }
    }

    static String choiceTask(String language, List<String> options) {
        String script = "hindi".equalsIgnoreCase(language) ? "Hindi (Devanagari script)" : "Tamil script";
        return "The picture shows one handwritten character in " + script + ", drawn by a learner with a finger or mouse "
                + "on a plain background. It is exactly one of these characters: " + String.join(" ", options)
                + ". Look closely at the strokes: some of these characters differ only by a small extra stroke, loop or mark. "
                + "For each of these characters give a confidence from 0 to 1 that it is the one drawn; the confidences "
                + "should add up to about 1. Only use characters from the list.";
    }

    static Map<String, Object> choiceSchema() {
        Map<String, Object> itemProperties = new LinkedHashMap<>();
        itemProperties.put("character", Map.of("type", "string"));
        itemProperties.put("confidence", Map.of("type", "number"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", itemProperties);
        item.put("required", List.of("character", "confidence"));
        item.put("additionalProperties", false);
        Map<String, Object> choices = new LinkedHashMap<>();
        choices.put("type", "array");
        choices.put("items", item);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("choices", choices);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("choices"));
        schema.put("additionalProperties", false);
        return schema;
    }
}
