package com.infotech.lang.application.proctor;

import com.infotech.lang.application.ai.AiRequest;
import java.util.List;
import java.util.Map;

/** Builds the instructions sent to the model. No student data is ever put into a prompt. */
public final class ProctorPromptBuilder {

    /** Bump when the instructions change so older texts can be regenerated. */
    public static final int PROMPT_VERSION = 1;

    private static final String SYSTEM = String.join("\n",
            "You write short, friendly on-screen messages for an automated language placement assessment.",
            "Write plain text only: no markdown, no emojis, no links.",
            "Never mention scores, results, passing, failing, marks, or whether any answer is right or wrong.",
            "Never give hints about answers.",
            "Reply only with the JSON object requested.");

    private ProctorPromptBuilder() {
    }

    public static AiRequest build(ProctorSlot slot, String testedLanguage) {
        String language = capitalize(testedLanguage);
        String task;
        if (slot.purpose() == ProctorPurpose.GREETING) {
            task = "Write a warm welcome for a student who is about to take the Level " + slot.level() + " " + language
                    + " placement assessment. It has five short activities: picture, word and letter recognition; handwriting;"
                    + " sentence translation; a short conversation; and video comprehension."
                    + " \"english\": two short sentences, at most 40 words, written in English, addressing the student by the"
                    + " exact placeholder {name} once. \"phrase\": one short, warm welcoming sentence written in " + language
                    + " script (not transliterated).";
        } else {
            task = "Write a short introduction to one activity of the Level " + slot.level() + " " + language
                    + " placement assessment. The activity is \"" + slot.activity().getLabel() + "\": "
                    + slot.activity().getDescription() + "."
                    + " \"english\": one or two sentences, at most 35 words, written in English, telling the student what to"
                    + " expect. Do not use the student's name. \"phrase\": one short, encouraging sentence written in "
                    + language + " script (not transliterated).";
        }
        return new AiRequest("PROCTOR_TEXT", SYSTEM, task, schema());
    }

    static Map<String, Object> schema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "english", Map.of("type", "string"),
                        "phrase", Map.of("type", "string")),
                "required", List.of("english", "phrase"),
                "additionalProperties", false);
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }
}
