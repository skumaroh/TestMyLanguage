package com.infotech.lang.application.translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Picks random simple English sentences for the translation block from a hand-written, owner-reviewed list
 * ({@code translation/level1-sentences.txt}). There is no reference translation: the Meta model judges the student's answer
 * for meaning. The same list serves both languages.
 */
@Service
public class SentencePicker {

    static final String RESOURCE = "translation/level1-sentences.txt";

    private final List<TranslationItem> all;

    public SentencePicker() {
        this.all = load();
    }

    /** Up to {@code count} different sentences, in random order. {@code language} is kept for per-language lists later. */
    public List<TranslationItem> pick(String language, int count) {
        List<TranslationItem> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, Math.min(count, shuffled.size())));
    }

    private static List<TranslationItem> load() {
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

    /** Sentences from the lines of the list; ids are 1, 2, 3 ... in file order. Blank lines and # comments are skipped. */
    static List<TranslationItem> parse(List<String> lines) {
        List<TranslationItem> items = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            items.add(new TranslationItem(items.size() + 1L, line, ""));
        }
        return items;
    }
}
