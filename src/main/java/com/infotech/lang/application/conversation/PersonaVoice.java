package com.infotech.lang.application.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;

/**
 * Who the persona sounds like: male or female, and a child, young, adult or old person. It comes from the persona's profile
 * ({@code gender}, {@code age_band}, or a numeric {@code age}); Meta writes these when it drafts a conversation and an admin can
 * correct them. Unknown values mean an adult of unspecified gender, which keeps the default voice.
 */
public record PersonaVoice(String gender, String ageBand) {

    public static final String FEMALE = "FEMALE";
    public static final String MALE = "MALE";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String CHILD = "CHILD";
    public static final String YOUNG = "YOUNG";
    public static final String ADULT = "ADULT";
    public static final String OLD = "OLD";

    public static PersonaVoice of(Conversation c) {
        JsonNode p = PersonaProfiles.profile(c);
        if (p == null) {
            return new PersonaVoice(UNKNOWN, ADULT);
        }
        String gender = p.path("gender").asText("").trim().toUpperCase(Locale.ROOT);
        gender = FEMALE.equals(gender) || MALE.equals(gender) ? gender : UNKNOWN;
        String band = p.path("age_band").asText("").trim().toUpperCase(Locale.ROOT);
        if (!java.util.List.of(CHILD, YOUNG, ADULT, OLD).contains(band)) {
            band = p.path("age").canConvertToInt() ? bandOf(p.path("age").asInt()) : ADULT;
        }
        return new PersonaVoice(gender, band);
    }

    static String bandOf(int age) {
        return age < 13 ? CHILD : age < 30 ? YOUNG : age < 60 ? ADULT : OLD;
    }

    /** For the admin page, for example "female, old". */
    public String describe() {
        return (UNKNOWN.equals(gender) ? "gender not set" : gender.toLowerCase(Locale.ROOT)) + ", " + ageBand.toLowerCase(Locale.ROOT);
    }
}
