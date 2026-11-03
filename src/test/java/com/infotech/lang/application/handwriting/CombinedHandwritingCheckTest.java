package com.infotech.lang.application.handwriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiResult;
import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CombinedHandwritingCheckTest {

    private static CombinedHandwritingCheck check(double local, String metaJson, boolean hasReference) {
        LetterShapeComparisonService shape = (drawn, ref) -> new LetterShapeComparisonService.ComparisonResult(local, local >= 0.5);
        HandwritingReader reader = new HandwritingReader(request -> metaJson == null
                ? AiResult.failure(503, 10, "HTTP 503")
                : AiResult.success(metaJson, 200, 10, 1, 1));
        ReferenceImages refs = letter -> hasReference ? Optional.of(new byte[] {1}) : Optional.empty();
        return new CombinedHandwritingCheck(shape, reader, refs, 0.7, 0.3, 0.8);
    }

    @Test
    void combineWeighsTheOldComparisonMore() {
        assertEquals(0.69, CombinedHandwritingCheck.combine(0.6, 0.9, 0.7, 0.3), 0.0001);
        assertEquals(0.87, CombinedHandwritingCheck.combine(0.9, 0.8, 0.7, 0.3), 0.0001);
        assertEquals(0.5, CombinedHandwritingCheck.combine(0.5, 0.5, 7, 3), 0.0001);
    }

    @Test
    void passesWhenBothAgreeAndTheWeightedResultIsHighEnough() {
        CombinedHandwritingCheck.Result r = check(0.9, "{\"character\":\"க\",\"confidence\":0.9}", true).check(new byte[] {1}, "tamil", "க");
        assertTrue(r.passed());
        assertEquals(0.9, r.combined(), 0.0001);
    }

    @Test
    void failsWhenMetaReadsADifferentLetterEvenWithHighConfidence() {
        CombinedHandwritingCheck.Result r = check(0.9, "{\"character\":\"ம\",\"confidence\":0.95}", true).check(new byte[] {1}, "tamil", "க");
        assertEquals(0.0, r.metaScore(), 0.0);
        assertEquals(0.63, r.combined(), 0.0001);
        assertFalse(r.passed());
    }

    @Test
    void aGoodDrawingWithAMiddlingLocalScoreMissesTheThreshold() {
        CombinedHandwritingCheck.Result r = check(0.6, "{\"character\":\"க\",\"confidence\":0.9}", true).check(new byte[] {1}, "tamil", "க");
        assertEquals(0.69, r.combined(), 0.0001);
        assertFalse(r.passed());
    }

    @Test
    void cannotPassWhenMetaDoesNotAnswer() {
        CombinedHandwritingCheck.Result r = check(1.0, null, true).check(new byte[] {1}, "tamil", "க");
        assertFalse(r.metaAnswered());
        assertFalse(r.passed());
    }

    @Test
    void cannotPassWithoutAReferenceImage() {
        CombinedHandwritingCheck.Result r = check(1.0, "{\"character\":\"க\",\"confidence\":1}", false).check(new byte[] {1}, "tamil", "க");
        assertNull(r.localScore());
        assertNull(r.combined());
        assertFalse(r.passed());
    }

    @Test
    void sameLetterIgnoresSpacesAndUnicodeCompositionDifferences() {
        assertTrue(CombinedHandwritingCheck.sameLetter(" é ", "é"));
        assertFalse(CombinedHandwritingCheck.sameLetter("a", "b"));
        assertFalse(CombinedHandwritingCheck.sameLetter(null, "b"));
    }
}
