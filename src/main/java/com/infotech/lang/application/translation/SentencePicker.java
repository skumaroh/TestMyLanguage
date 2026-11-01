package com.infotech.lang.application.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Picks random simple sentences for the translation block from the checked sentences of the existing sentence table:
 * the shortest {@code translation.pool-size} English sentences of the student's language, then a random selection. Read-only.
 */
@Service
public class SentencePicker {

    private static final int FETCH_LIMIT = 400;

    private final TranslationSentenceRepository sentences;
    private final int poolSize;

    public SentencePicker(TranslationSentenceRepository sentences, @Value("${translation.pool-size:40}") int poolSize) {
        this.sentences = sentences;
        this.poolSize = poolSize;
    }

    public List<TranslationItem> pick(String language, int count) {
        List<TranslationItem> pool = new ArrayList<>();
        for (TranslationSentence s : sentences.findCheckedShortestFirst(PageRequest.of(0, FETCH_LIMIT))) {
            if (languageMatches(s.getLanguageCode(), language) && pool.size() < poolSize) {
                pool.add(new TranslationItem(s.getId(), s.getNaturalTranslation().trim(), s.getSentenceText().trim()));
            }
        }
        Collections.shuffle(pool);
        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }

    /**
     * True when a stored language code (for example {@code ta}, {@code ta-IN}, {@code tamil} or empty) belongs to the language
     * ({@code tamil} or {@code hindi}). A blank code is accepted: each language has its own database.
     */
    static boolean languageMatches(String languageCode, String language) {
        if (languageCode == null || languageCode.isBlank() || language == null || language.isBlank()) {
            return true;
        }
        String code = languageCode.trim().toLowerCase(Locale.ROOT);
        String lang = language.trim().toLowerCase(Locale.ROOT);
        String iso = lang.startsWith("ta") ? "ta" : lang.startsWith("hi") ? "hi" : lang;
        return code.equals(lang) || code.equals(iso) || code.startsWith(iso + "-") || code.startsWith(iso + "_");
    }
}
