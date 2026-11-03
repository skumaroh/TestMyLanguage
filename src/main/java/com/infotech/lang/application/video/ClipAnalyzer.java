package com.infotech.lang.application.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiRequest;
import com.infotech.lang.application.ai.AiResult;
import com.infotech.lang.application.conversation.ConversationGenerator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Lets the Meta model watch a clip (or look at a picture) and draft what a Level 1 learner should say: a very simple English sentence,
 * the same in the tested language, accepted variants, whether the action is clear enough to recognise without guessing, and anything
 * that could confuse a learner. Java checks the answer (the tested-language text really is in that script) before anything is saved,
 * and the result is a DRAFT: only an admin's verification lets a clip be used in a test.
 */
@Service
public class ClipAnalyzer {

    public static final int PROMPT_VERSION = 1;
    static final int MAX_CHARS = 200;

    static final String SYSTEM = "You prepare answers for a beginner language-learning test about short clips and pictures, and answer only "
            + "with the requested JSON. Describe only what is plainly visible; never guess.";

    private final AiGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClipAnalyzer(AiGateway gateway) {
        this.gateway = gateway;
    }

    /** The draft, or empty when the model could not be reached or its answer failed the checks. Never throws. */
    public Optional<ClipDraft> analyze(String language, byte[] media, boolean video) {
        AiResult result;
        try {
            result = gateway.completeJson(AiRequest.withMedia("CLIP_ANALYZE", SYSTEM, task(language, video), schema(), media, video, 90));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (!result.ok()) {
            return Optional.empty();
        }
        return parse(result.content(), language);
    }

    Optional<ClipDraft> parse(String json, String language) {
        try {
            JsonNode root = mapper.readTree(json);
            String english = clip(root.path("english").asText(""));
            String expected = clip(root.path("expected_text").asText(""));
            if (english.isEmpty() || !ConversationGenerator.inScript(expected, language)) {
                return Optional.empty();
            }
            List<String> variants = new ArrayList<>();
            for (JsonNode v : root.path("variants")) {
                String text = clip(v.asText(""));
                if (!text.isEmpty() && !text.equals(expected) && !text.contains(";") && ConversationGenerator.inScript(text, language)) {
                    variants.add(text);
                }
            }
            boolean clear = "YES".equalsIgnoreCase(root.path("clear_enough").asText(""));
            String difficulty = "MEDIUM".equalsIgnoreCase(root.path("difficulty").asText("")) ? "MEDIUM" : "EASY";
            return Optional.of(new ClipDraft(english, expected.replace(";", ""), variants.stream().limit(3).toList(), clear,
                    clip(root.path("problems").asText("")), difficulty));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String clip(String s) {
        String t = s == null ? "" : s.trim();
        return t.length() > MAX_CHARS ? t.substring(0, MAX_CHARS) : t;
    }

    static String task(String language, boolean video) {
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi (Devanagari script)" : "Tamil (Tamil script)";
        String what = video ? "clip" : "picture";
        return "This " + what + " will be shown to a beginner (Level 1) learner of " + name + ", who must say what is happening.\n"
                + "Give:\n"
                + "- english: one very simple present-tense sentence naming who or what and the action, for example \"The boy is swimming.\"\n"
                + "- expected_text: the same sentence in " + name + ", very simple, natural, the form a teacher would expect first.\n"
                + "- variants: up to 3 other correct ways to say it in " + name + ": the other gender form of the verb or the habitual form, "
                + "and, as one entry, the single most important word on its own (for example just the noun, such as the word for 'fire' "
                + "for a crackling fire), because a learner may only know that word.\n"
                + "- clear_enough: YES if the action is recognisable without guessing, NO if it is ambiguous, shows several actions, or is "
                + "hard to see.\n"
                + "- problems: anything that could confuse a learner (clutter, text or letters in the " + what + ", a real person's face, "
                + "two people, an unclear action), or \"none\".\n"
                + "- difficulty: EASY for a common everyday action, MEDIUM otherwise.";
    }

    static Map<String, Object> schema() {
        Map<String, Object> str = new LinkedHashMap<>();
        str.put("type", "string");
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("type", "array");
        list.put("items", str);
        Map<String, Object> yesNo = new LinkedHashMap<>();
        yesNo.put("type", "string");
        yesNo.put("enum", List.of("YES", "NO"));
        Map<String, Object> difficulty = new LinkedHashMap<>();
        difficulty.put("type", "string");
        difficulty.put("enum", List.of("EASY", "MEDIUM"));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("english", str);
        props.put("expected_text", str);
        props.put("variants", list);
        props.put("clear_enough", yesNo);
        props.put("problems", str);
        props.put("difficulty", difficulty);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", new ArrayList<>(props.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }
}
