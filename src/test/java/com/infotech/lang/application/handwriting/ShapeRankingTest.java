package com.infotech.lang.application.handwriting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShapeRankingTest {

    /** A fake comparison: the "reference picture" is one byte holding the score in hundredths. */
    private static final LetterShapeComparisonService FAKE =
            (drawn, ref) -> new LetterShapeComparisonService.ComparisonResult(ref[0] / 100.0, ref[0] >= 50);

    private static LetterCandidate candidate(String letter, int hundredths) {
        return new LetterCandidate(letter, new byte[] {(byte) hundredths});
    }

    @Test
    void ranksTheBestScoreFirst() {
        List<ShapeRanking.Ranked> ranked = ShapeRanking.rank(FAKE, new byte[] {1},
                List.of(candidate("அ", 78), candidate("ஆ", 80), candidate("இ", 40)));
        assertEquals("ஆ", ranked.get(0).letter());
        assertEquals("அ", ranked.get(1).letter());
        assertEquals("இ", ranked.get(2).letter());
        assertEquals(0.02, ShapeRanking.margin(ranked), 0.0001);
    }

    @Test
    void aCandidateThatCannotBeComparedScoresZero() {
        LetterShapeComparisonService failing = (drawn, ref) -> {
            throw new IllegalArgumentException("bad image");
        };
        List<ShapeRanking.Ranked> ranked = ShapeRanking.rank(failing, new byte[] {1}, List.of(candidate("அ", 80)));
        assertEquals(0.0, ranked.get(0).score(), 0.0);
    }

    @Test
    void marginIsZeroWithFewerThanTwoCandidates() {
        assertEquals(0.0, ShapeRanking.margin(List.of()), 0.0);
        assertEquals(0.0, ShapeRanking.margin(ShapeRanking.rank(FAKE, new byte[] {1}, List.of(candidate("அ", 80)))), 0.0);
    }
}
