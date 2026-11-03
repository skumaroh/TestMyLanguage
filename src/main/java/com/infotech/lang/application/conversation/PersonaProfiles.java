package com.infotech.lang.application.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The persona's "small brain". A conversation's persona description may itself be a JSON profile (an admin pastes it into the
 * persona box); when it is plain text, the built-in profile whose keywords appear in the scenario or description is used (the
 * milk vendor first); otherwise the plain text alone. The profile is data given to the model, never instructions from a learner.
 */
public final class PersonaProfiles {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<Entry> BUILT_IN = load();

    private record Entry(List<String> keywords, String profileJson) {
    }

    private PersonaProfiles() {
    }

    /**
     * The persona's profile as one JSON object: the built-in profile matched by keyword (if any) with the conversation's own JSON
     * description laid over it (its keys win). Null when the description is plain text and no built-in profile matches.
     */
    public static JsonNode profile(Conversation c) {
        String desc = c.getPersonaDescription() == null ? "" : c.getPersonaDescription().trim();
        ObjectNode merged = null;
        String builtIn = builtIn(c.getScenarioTitle() + " " + desc);
        if (builtIn != null) {
            merged = parseObject(builtIn);
        }
        ObjectNode own = desc.startsWith("{") ? parseObject(desc) : null;
        if (own != null) {
            if (merged == null) {
                merged = own;
            } else {
                merged.setAll(own);
            }
        }
        return merged;
    }

    /** The paragraph that tells the model who it is and what it knows. */
    public static String describe(Conversation c) {
        String desc = c.getPersonaDescription() == null ? "" : c.getPersonaDescription().trim();
        JsonNode profile = profile(c);
        if (profile == null) {
            return "About you: " + desc + ".";
        }
        StringBuilder sb = new StringBuilder("Your profile (this is everything you know about yourself and your work; speak as this person, "
                + "and answer questions about yourself or your work from it):\n").append(profile.toString());
        if (!desc.startsWith("{") && !desc.isEmpty()) {
            sb.append("\nAbout you: ").append(desc).append('.');
        }
        return sb.toString();
    }

    static String builtIn(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (Entry e : BUILT_IN) {
            for (String k : e.keywords()) {
                if (t.contains(k.toLowerCase(Locale.ROOT))) {
                    return e.profileJson();
                }
            }
        }
        return null;
    }

    private static ObjectNode parseObject(String s) {
        try {
            JsonNode n = MAPPER.readTree(s);
            return n.isObject() ? (ObjectNode) n : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Entry> load() {
        List<Entry> out = new ArrayList<>();
        try (InputStream in = PersonaProfiles.class.getResourceAsStream("/conversation/personas.json")) {
            if (in == null) {
                return out;
            }
            for (JsonNode n : MAPPER.readTree(in)) {
                List<String> keys = new ArrayList<>();
                n.path("keywords").forEach(k -> keys.add(k.asText()));
                out.add(new Entry(keys, n.path("profile").toString()));
            }
        } catch (Exception e) {
            // no built-in profiles: personas then use their plain description
        }
        return out;
    }
}
