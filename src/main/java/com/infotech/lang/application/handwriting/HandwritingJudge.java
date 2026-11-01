package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Decides whether a drawing is the expected letter, using only the old shape comparison (owner decision, 2026-11-01: the Meta
 * vision model misread the test drawings, so it is not part of the score). The drawing is compared with every letter of the
 * expected letter's group; it is right when the expected letter scores best, at or above a minimum score, with a minimum lead.
 */
@Service
public class HandwritingJudge {

    private static final int MAX_CANDIDATES = 40;

    /**
     * @param correct   true when the drawing counts as the expected letter
     * @param judged    false when the check could not be made (no reference images to compare with)
     * @param best      the letter that scored best
     * @param score     the score of the expected letter
     * @param lead      how far the best letter leads the second best
     * @param rank      the place of the expected letter, 1 is best (0 when unknown)
     */
    public record Verdict(boolean correct, boolean judged, String best, double score, double lead, int rank) {

        /** A short, picture-free description for the answer log. */
        public String summary(String expected) {
            return String.format(Locale.ROOT, "%s meant=%s best=%s score=%.3f lead=%.3f rank=%d", judged ? "judged" : "unjudged",
                    expected, best, score, lead, rank);
        }
    }

    private final LetterShapeComparisonService shape;
    private final ReferenceImages references;
    private final double minScore;
    private final double minLead;

    public HandwritingJudge(LetterShapeComparisonService shape,
                            ReferenceImages references,
                            @Value("${handwriting.rule.min-score:0.70}") double minScore,
                            @Value("${handwriting.rule.min-lead:0.0}") double minLead) {
        this.shape = shape;
        this.references = references;
        this.minScore = minScore;
        this.minLead = minLead;
    }

    public Verdict judge(byte[] drawingPng, String expectedLetter) {
        List<LetterCandidate> candidates = references.candidatesFor(expectedLetter, MAX_CANDIDATES);
        boolean hasExpected = candidates.stream().anyMatch(c -> c.letter().equals(expectedLetter.trim()));
        if (candidates.size() < 2 || !hasExpected) {
            return new Verdict(false, false, "", 0, 0, 0);
        }
        List<ShapeRanking.Ranked> ranked = ShapeRanking.rank(shape, drawingPng, candidates);
        int rank = 0;
        double score = 0;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).letter().equals(expectedLetter.trim())) {
                rank = i + 1;
                score = ranked.get(i).score();
                break;
            }
        }
        double lead = ShapeRanking.margin(ranked);
        boolean correct = rank == 1 && score >= minScore && lead >= minLead;
        return new Verdict(correct, true, ranked.get(0).letter(), score, lead, rank);
    }
}
