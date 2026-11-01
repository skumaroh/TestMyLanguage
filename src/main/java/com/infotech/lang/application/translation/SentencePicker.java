package com.infotech.lang.application.translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Picks random simple English sentences for the translation block from a hand-written list
 * ({@code translation/level1-sentences.txt}: {@code English | Tamil | Hindi}, extra accepted forms after {@code ;}).
 * Even positions (1st, 3rd ...) are filled first with sentences that have a stored answer in the language, because the
 * block asks those as word-tile questions; the rest are typed questions.
 */
@Service
public class SentencePicker {

    static final String RESOURCE = "translation/level1-sentences.txt";

    /** One line of the list: the English sentence and its accepted Tamil and Hindi answers (first = canonical). */
    record Entry(long id, String english, List<String> tamil, List<String> hindi) {
        TranslationItem forLanguage(String language) {
            List<String> forms = "hindi".equalsIgnoreCase(language) ? hindi : tamil;
            if (forms.isEmpty()) {
                return new TranslationItem(id, english, "", List.of());
            }
            return new TranslationItem(id, english, forms.get(0), forms.subList(1, forms.size()));
        }
    }

    private final List<Entry> all;

    public SentencePicker() {
        this.all = load();
    }

    /** Every sentence of the list for the language (used to find decoy words for the tile questions). */
    public List<TranslationItem> allFor(String language) {
        List<TranslationItem> items = new ArrayList<>();
        for (Entry e : all) {
            items.add(e.forLanguage(language));
        }
        return items;
    }

    /** Up to {@code count} different sentences, in random order, with answer-bearing ones at the even positions. */
    public List<TranslationItem> pick(String language, int count) {
        List<TranslationItem> shuffled = allFor(language);
        Collections.shuffle(shuffled);
        List<TranslationItem> withAnswer = new ArrayList<>();
        List<TranslationItem> without = new ArrayList<>();
        for (TranslationItem item : shuffled) {
            (item.hasAnswer() ? withAnswer : without).add(item);
        }
        List<TranslationItem> tiles = new ArrayList<>(withAnswer.subList(0, Math.min((count + 1) / 2, withAnswer.size())));
        List<TranslationItem> rest = new ArrayList<>(withAnswer.subList(tiles.size(), withAnswer.size()));
        rest.addAll(without);
        Collections.shuffle(rest);
        List<TranslationItem> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean tileSlot = i % 2 == 0;
            List<TranslationItem> first = tileSlot ? tiles : rest;
            List<TranslationItem> second = tileSlot ? rest : tiles;
            if (!first.isEmpty()) {
                result.add(first.remove(0));
            } else if (!second.isEmpty()) {
                result.add(second.remove(0));
            } else {
                break;
            }
        }
        return result;
    }

    private static List<Entry> load() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parse(lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    /** Entries from the lines of the list; ids are 1, 2, 3 ... in file order. Blank lines and # comments are skipped. */
    static List<Entry> parse(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            entries.add(new Entry(entries.size() + 1L, parts[0].trim(),
                    parts.length > 1 ? forms(parts[1]) : List.of(),
                    parts.length > 2 ? forms(parts[2]) : List.of()));
        }
        return entries;
    }

    private static List<String> forms(String field) {
        return Arrays.stream(field.split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
