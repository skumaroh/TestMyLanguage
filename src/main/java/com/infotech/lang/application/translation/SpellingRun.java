package com.infotech.lang.application.translation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * A suggestion for one word or a run of neighbouring words: the text now shown between {@code start} and {@code end} and the
 * text that should replace exactly that span.
 */
public record SpellingRun(int start, int end, String currentText, String suggestion) {

    /**
     * Groups the words whose suggested spelling differs from what is shown into runs of neighbouring words (separated by a
     * single space in the box). {@code scripts} holds one suggested spelling per word, in the same order.
     */
    public static List<SpellingRun> group(List<TypedWord> words, List<String> scripts) {
        List<SpellingRun> runs = new ArrayList<>();
        if (words.size() != scripts.size()) {
            return runs;
        }
        int i = 0;
        while (i < words.size()) {
            if (!differs(words.get(i), scripts.get(i))) {
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < words.size() && differs(words.get(j + 1), scripts.get(j + 1))
                    && words.get(j + 1).start() == words.get(j).end() + 1) {
                j++;
            }
            StringBuilder shown = new StringBuilder();
            StringBuilder suggested = new StringBuilder();
            for (int k = i; k <= j; k++) {
                if (k > i) {
                    shown.append(' ');
                    suggested.append(' ');
                }
                shown.append(words.get(k).shown());
                suggested.append(scripts.get(k).trim());
            }
            runs.add(new SpellingRun(words.get(i).start(), words.get(j).end(), shown.toString(), suggested.toString()));
            i = j + 1;
        }
        return runs;
    }

    private static boolean differs(TypedWord word, String script) {
        String suggestion = script == null ? "" : script.trim();
        return !suggestion.isEmpty() && !suggestion.contains(" ")
                && !suggestion.matches(".*[A-Za-z].*")
                && !normalize(suggestion).equals(normalize(word.shown()));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFC);
    }
}
