package com.infotech.lang.application.assessment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Picks random words for the words block (house content, one level) and builds their tiles. Read-only. */
@Service
public class WordQuestionService {

    private final AssessWordRepository words;
    private final AssessLettersRepository letters;
    private final int tileCount;
    private final Random random = new Random();

    public WordQuestionService(AssessWordRepository words,
                               AssessLettersRepository letters,
                               @Value("${assessment.words.tile-count:12}") int tileCount) {
        this.words = words;
        this.letters = letters;
        this.tileCount = tileCount;
    }

    /** Up to {@code count} different random words; fewer only if the content has fewer. */
    public List<WordQuestion> pick(int count, int level) {
        List<AssessWord> all = new ArrayList<>(words.findHouseWords(level));
        all.removeIf(w -> w.getAnswerText() == null || w.getAnswerText().isBlank());
        Collections.shuffle(all, random);
        List<String> pool = letters.findAll().stream().findFirst()
                .map(l -> TileBuilder.split(l.getLetters())).orElse(List.of());
        List<WordQuestion> picked = new ArrayList<>();
        for (AssessWord w : all.subList(0, Math.min(count, all.size()))) {
            picked.add(new WordQuestion(w.getId(), w.getQuestionText(), w.getAnswerText().trim(),
                    TileBuilder.tilesFor(w.getAnswerOptions(), w.getAnswerText(), pool, tileCount, random),
                    blankToNull(w.getImageUrl()), blankToNull(w.getAudioUrl())));
        }
        return picked;
    }

    /** True when the built text equals the word, ignoring surrounding spaces and Unicode composition differences. */
    public static boolean isCorrect(String given, String answer) {
        if (given == null || answer == null) {
            return false;
        }
        return Normalizer.normalize(given.trim(), Normalizer.Form.NFC)
                .equals(Normalizer.normalize(answer.trim(), Normalizer.Form.NFC));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
