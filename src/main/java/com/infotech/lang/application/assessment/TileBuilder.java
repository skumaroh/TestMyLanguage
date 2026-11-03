package com.infotech.lang.application.assessment;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Builds the letter tiles for a word: the word's own letters plus random extra letters, shuffled. */
final class TileBuilder {

    private TileBuilder() {
    }

    /** Splits a comma-separated list such as {@code "க,ம,ல"} into trimmed, non-empty, distinct entries. */
    static List<String> split(String commaSeparated) {
        Set<String> out = new LinkedHashSet<>();
        if (commaSeparated != null) {
            for (String part : commaSeparated.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** Letters of the answer when the word has no letter list: one entry per user-perceived character. */
    static List<String> graphemes(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text != null) {
            BreakIterator it = BreakIterator.getCharacterInstance();
            it.setText(text);
            int start = it.first();
            for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
                String g = text.substring(start, end).trim();
                if (!g.isEmpty()) {
                    out.add(g);
                }
            }
        }
        return new ArrayList<>(out);
    }

    static List<String> tilesFor(String answerOptions, String answerText, List<String> pool, int tileCount, Random random) {
        Set<String> tiles = new LinkedHashSet<>(split(answerOptions));
        if (tiles.isEmpty()) {
            tiles.addAll(graphemes(answerText));
        }
        List<String> extras = new ArrayList<>(pool);
        Collections.shuffle(extras, random);
        for (String extra : extras) {
            if (tiles.size() >= tileCount) {
                break;
            }
            tiles.add(extra);
        }
        List<String> result = new ArrayList<>(tiles);
        Collections.shuffle(result, random);
        return result;
    }
}
