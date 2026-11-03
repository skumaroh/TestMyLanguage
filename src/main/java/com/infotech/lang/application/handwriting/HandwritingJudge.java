package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Decides whether a drawing is the expected letter, using only the old shape comparison (owner decision, 2026-11-01: the Meta
 * vision model misread the test drawings, so it is not part of the score). The drawing is compared with every letter of the
 * expected letter's group; it is right when the expected letter scores at or above a minimum score and is the best or within a small
 * margin of the best (a clean drawing of a simple letter can lose a close race to a similar-looking letter, and should not be
 * sent back for it), with a minimum lead for the best letter only when the expected letter is the best.
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
     * @param gap       how far the expected letter's score is below the best score (0 when it is the best)
     */
    public record Verdict(boolean correct, boolean judged, String best, double score, double lead, int rank, double gap) {

        public Verdict(boolean correct, boolean judged, String best, double score, double lead, int rank) {
            this(correct, judged, best, score, lead, rank, 0);
        }

        /** A short, picture-free description for the answer log. */
        public String summary(String expected) {
            return String.format(Locale.ROOT, "%s meant=%s best=%s score=%.3f lead=%.3f rank=%d gap=%.3f", judged ? "judged" : "unjudged",
                    expected, best, score, lead, rank, gap);
        }
    }

    private final LetterShapeComparisonService shape;
    private final ReferenceImages references;
    private final double minScore;
    private final double minLead;
    private final double closeMargin;
    private final boolean useVariants;

    @Autowired
    public HandwritingJudge(LetterShapeComparisonService shape,
                            ReferenceImages references,
                            @Value("${handwriting.rule.min-score:0.70}") double minScore,
                            @Value("${handwriting.rule.min-lead:0.0}") double minLead,
                            @Value("${handwriting.rule.close-margin:0.05}") double closeMargin,
                            @Value("${handwriting.rule.variants:true}") boolean useVariants) {
        this.shape = shape;
        this.references = references;
        this.minScore = minScore;
        this.minLead = minLead;
        this.closeMargin = Math.max(0, closeMargin);
        this.useVariants = useVariants;
    }

    /** The strict rule (no margin, no variants), as before. */
    public HandwritingJudge(LetterShapeComparisonService shape, ReferenceImages references, double minScore, double minLead) {
        this(shape, references, minScore, minLead, 0.0, false);
    }

    public Verdict judge(byte[] drawingPng, String expectedLetter) {
        List<LetterCandidate> candidates = references.candidatesFor(expectedLetter, MAX_CANDIDATES);
        boolean hasExpected = candidates.stream().anyMatch(c -> c.letter().equals(expectedLetter.trim()));
        if (candidates.size() < 2 || !hasExpected) {
            return new Verdict(false, false, "", 0, 0, 0);
        }
        List<ShapeRanking.Ranked> ranked = ShapeRanking.rank(shape, drawingPng, candidates, useVariants);
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
        double gap = rank == 0 ? 1.0 : ranked.get(0).score() - score;
        boolean isBest = rank == 1 && lead >= minLead;
        boolean closeEnough = rank > 1 && gap <= closeMargin + 1e-9;
        boolean correct = rank > 0 && score >= minScore && (isBest || closeEnough);
        return new Verdict(correct, true, ranked.get(0).letter(), score, lead, rank, rank == 0 ? 0 : gap);
    }
}
