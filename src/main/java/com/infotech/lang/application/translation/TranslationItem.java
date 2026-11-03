package com.infotech.lang.application.translation;

import java.util.ArrayList;
import java.util.List;

/**
 * One sentence to translate: the English prompt, the canonical target-language answer (may be blank) and other accepted
 * forms such as the feminine verb. Answers are in the language the item was made for.
 */
public record TranslationItem(long id, String english, String expected, List<String> variants) {

    public TranslationItem(long id, String english, String expected) {
        this(id, english, expected, List.of());
    }

    public boolean hasAnswer() {
        return expected != null && !expected.isBlank();
    }

    /** The canonical answer followed by the other accepted forms. */
    public List<String> accepted() {
        List<String> all = new ArrayList<>();
        if (hasAnswer()) {
            all.add(expected);
        }
        all.addAll(variants);
        return all;
    }
}
