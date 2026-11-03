package com.infotech.lang.application.handwriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HandwritingJudgeTest {

    /** The fake "reference picture" is one byte holding the score in hundredths. */
    private static final LetterShapeComparisonService SHAPE =
            (drawn, ref) -> new LetterShapeComparisonService.ComparisonResult(ref[0] / 100.0, ref[0] >= 50);

    private static LetterCandidate candidate(String letter, int hundredths) {
        return new LetterCandidate(letter, new byte[] {(byte) hundredths});
    }

    private static HandwritingJudge judgeWith(List<LetterCandidate> candidates, double minScore, double minLead) {
        ReferenceImages refs = new ReferenceImages() {
            @Override
            public Optional<byte[]> pngFor(String letter) {
                return Optional.empty();
            }

            @Override
            public List<LetterCandidate> candidatesFor(String expectedLetter, int max) {
                return candidates;
            }
        };
        return new HandwritingJudge(SHAPE, refs, minScore, minLead);
    }

    private static final List<LetterCandidate> VOWELS = List.of(candidate("அ", 82), candidate("இ", 81), candidate("ஆ", 76));

    @Test
    void correctWhenTheExpectedLetterScoresBestAndHighEnough() {
        HandwritingJudge.Verdict v = judgeWith(VOWELS, 0.70, 0.0).judge(new byte[] {1}, "அ");
        assertTrue(v.correct());
        assertTrue(v.judged());
        assertEquals(1, v.rank());
        assertEquals(0.01, v.lead(), 0.0001);
    }

    @Test
    void wrongWhenAnotherLetterScoresBetter() {
        HandwritingJudge.Verdict v = judgeWith(VOWELS, 0.70, 0.0).judge(new byte[] {1}, "ஆ");
        assertFalse(v.correct());
        assertEquals(3, v.rank());
        assertEquals("அ", v.best());
    }

    @Test
    void wrongWhenTheBestScoreIsBelowTheMinimum() {
        assertFalse(judgeWith(VOWELS, 0.90, 0.0).judge(new byte[] {1}, "அ").correct());
    }

    @Test
    void wrongWhenTheLeadIsTooSmall() {
        assertFalse(judgeWith(VOWELS, 0.70, 0.05).judge(new byte[] {1}, "அ").correct());
    }

    @Test
    void notJudgedWithoutEnoughReferenceImages() {
        HandwritingJudge.Verdict v = judgeWith(List.of(candidate("அ", 90)), 0.70, 0.0).judge(new byte[] {1}, "அ");
        assertFalse(v.judged());
        assertFalse(v.correct());
        assertTrue(v.summary("அ").startsWith("unjudged"));
    }
}
