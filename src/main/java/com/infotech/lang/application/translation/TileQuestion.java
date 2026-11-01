package com.infotech.lang.application.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A word-tile question: the words of the stored answer plus a few decoy words taken from other sentences, shuffled. The
 * student taps the words in order. The tapped order is compared with the stored answer and its accepted forms; if it is
 * different the caller asks the grader for the meaning (another word order can still be correct).
 */
public record TileQuestion(List<String> tiles) {

    static final int DECOYS = 3;

    /** Tile words of an answer: split on spaces, final punctuation removed. */
    static List<String> words(String sentence) {
        List<String> words = new ArrayList<>();
        for (String w : sentence.trim().split("\\s+")) {
            String cleaned = w.replaceAll("^[\"'“‘(\\[]+", "").replaceAll("[\"'”’)\\].!?।,;:]+$", "");
            if (!cleaned.isEmpty()) {
                words.add(cleaned);
            }
        }
        return words;
    }

    /** Builds the tiles for {@code item}; {@code pool} is every sentence of the language (decoys come from the others). */
    public static TileQuestion build(TranslationItem item, List<TranslationItem> pool) {
        Set<String> own = new LinkedHashSet<>();
        for (String form : item.accepted()) {
            own.addAll(words(form));
        }
        Set<String> decoyCandidates = new LinkedHashSet<>();
        for (TranslationItem other : pool) {
            if (other.id() != item.id() && other.hasAnswer()) {
                for (String w : words(other.expected())) {
                    if (!own.contains(w)) {
                        decoyCandidates.add(w);
                    }
                }
            }
        }
        List<String> decoys = new ArrayList<>(decoyCandidates);
        Collections.shuffle(decoys);
        List<String> tiles = new ArrayList<>(words(item.expected()));
        tiles.addAll(decoys.subList(0, Math.min(DECOYS, decoys.size())));
        List<String> correct = words(item.expected());
        for (int attempt = 0; attempt < 10; attempt++) {
            Collections.shuffle(tiles);
            if (!tiles.equals(correct)) {
                break;
            }
        }
        return new TileQuestion(List.copyOf(tiles));
    }

    /** True when the tapped words, in order, are the canonical answer or one of its accepted forms. */
    public static boolean matches(TranslationItem item, List<String> tapped) {
        String given = TranslationGrader.normalize(String.join(" ", tapped));
        for (String form : item.accepted()) {
            if (TranslationGrader.normalize(String.join(" ", words(form))).equals(given)) {
                return true;
            }
        }
        return false;
    }
}
