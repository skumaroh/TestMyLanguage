package com.infotech.lang.application.handwriting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Picks random Level 1 letters that have a reference image: Tamil by its numeric level, Hindi by category. Read-only. */
@Service
public class LetterPicker {

    private final ReferenceLetterRepository letters;

    public LetterPicker(ReferenceLetterRepository letters) {
        this.letters = letters;
    }

    /** Up to {@code count} different letters, in random order. */
    public List<String> pick(String language, int count) {
        List<ReferenceLetter> rows = "hindi".equalsIgnoreCase(language)
                ? letters.findLevelOneByCategory()
                : letters.findLevelOneByNumericLevel();
        Set<String> distinct = new LinkedHashSet<>();
        for (ReferenceLetter row : rows) {
            if (row.getLetter() != null && !row.getLetter().isBlank()) {
                distinct.add(row.getLetter().trim());
            }
        }
        List<String> all = new ArrayList<>(distinct);
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }
}
