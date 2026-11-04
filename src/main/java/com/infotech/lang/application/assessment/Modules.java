package com.infotech.lang.application.assessment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The six modules of the assessment, in the order they are taken: the block code, the page, and the name shown to the student. */
public enum Modules {
    WORDS("WORDS", 1, "Picture, word and letter recognition", "assessment/words"),
    DRAW_LETTERS("DRAW_LETTERS", 2, "Writing the letters", "assessment/letters"),
    AUDIO_WORDS("AUDIO_WORDS", 3, "Listening and building words", "assessment/audio-words"),
    TRANSLATION("TRANSLATION", 4, "Translating sentences", "assessment/translation"),
    CONVERSATION("CONVERSATION", 5, "Conversation", "assessment/conversation"),
    VIDEO("VIDEO", 6, "Describing a video", "assessment/video");

    private final String code;
    private final int number;
    private final String title;
    private final String route;

    Modules(String code, int number, String title, String route) {
        this.code = code;
        this.number = number;
        this.title = title;
        this.route = route;
    }

    public String code() {
        return code;
    }

    public int number() {
        return number;
    }

    public String title() {
        return title;
    }

    public String route() {
        return route;
    }

    /** For example "Module 2 – Writing the letters". */
    public String heading() {
        return "Module " + number + " – " + title;
    }

    /** The module after this one, if there is one. */
    public Optional<Modules> following() {
        Modules[] all = values();
        return number < all.length ? Optional.of(all[number]) : Optional.empty();
    }

    /** The modules this student takes: all six, or five when no video clips are ready. */
    public static List<Modules> taken(boolean videoAvailable) {
        return java.util.Arrays.stream(values()).filter(m -> m != VIDEO || videoAvailable).toList();
    }

    /** The first module not yet finished, if any. */
    public static Optional<Modules> firstUnfinished(Set<String> finishedCodes, boolean videoAvailable) {
        return taken(videoAvailable).stream().filter(m -> !finishedCodes.contains(m.code)).findFirst();
    }
}
