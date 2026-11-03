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

    @Test
    void aCloseSecondPlaceCountsWhenAboveTheMinimumButAFarOneDoesNot() {
        // ப scores 0.80, the similar letter 0.83: not the best, but within 0.05
        List<LetterCandidate> group = List.of(candidate("ய", 83), candidate("ப", 80), candidate("ட", 60));
        HandwritingJudge lenient = new HandwritingJudge(SHAPE, refsOf(group), 0.70, 0.0, 0.05, false);
        HandwritingJudge.Verdict v = lenient.judge(new byte[] {1}, "ப");
        assertTrue(v.correct());
        assertEquals(2, v.rank());
        assertEquals(0.03, v.gap(), 0.0001);
        // the strict rule (no margin) still rejects it
        assertFalse(new HandwritingJudge(SHAPE, refsOf(group), 0.70, 0.0).judge(new byte[] {1}, "ப").correct());
        // 0.06 behind is too far, and a close second below the minimum score is still wrong
        assertFalse(lenient.judge(new byte[] {1}, "ட").correct());
        List<LetterCandidate> lowScores = List.of(candidate("ய", 68), candidate("ப", 66));
        assertFalse(new HandwritingJudge(SHAPE, refsOf(lowScores), 0.70, 0.0, 0.05, false).judge(new byte[] {1}, "ப").correct());
    }

    private static ReferenceImages refsOf(List<LetterCandidate> candidates) {
        return new ReferenceImages() {
            @Override
            public Optional<byte[]> pngFor(String letter) {
                return Optional.empty();
            }

            @Override
            public List<LetterCandidate> candidatesFor(String expectedLetter, int max) {
                return candidates;
            }
        };
    }
}
