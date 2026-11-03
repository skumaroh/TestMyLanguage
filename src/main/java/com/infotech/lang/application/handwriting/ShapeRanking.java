package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Makes the old shape comparison a recogniser: the drawing is compared with the reference image of every candidate letter,
 * and the letters are ranked by score. A drawing is then "this letter" only when that letter scores best.
 */
public final class ShapeRanking {

    public record Ranked(String letter, double score) {
    }

    private ShapeRanking() {
    }

    /** Candidates sorted from the best score to the worst. A candidate that cannot be compared scores 0. */
    public static List<Ranked> rank(LetterShapeComparisonService shape, byte[] drawingPng, List<LetterCandidate> candidates) {
        return rank(shape, drawingPng, candidates, false);
    }

    /**
     * Same, and with {@code useVariants} each candidate is also tried in a bolder and a thinner version of its reference image; the
     * best of the versions is the candidate's score.
     */
    public static List<Ranked> rank(LetterShapeComparisonService shape, byte[] drawingPng, List<LetterCandidate> candidates,
                                    boolean useVariants) {
        List<Ranked> ranked = new ArrayList<>();
        for (LetterCandidate candidate : candidates) {
            double score = 0;
            List<byte[]> references = useVariants ? ReferenceVariants.of(candidate.png()) : List.of(candidate.png());
            if (references.isEmpty()) {
                references = List.of(candidate.png());
            }
            for (byte[] reference : references) {
                try {
                    score = Math.max(score, shape.compare(drawingPng, reference).similarityScore());
                } catch (RuntimeException e) {
                    // this version cannot be compared: it scores 0
                }
            }
            ranked.add(new Ranked(candidate.letter(), score));
        }
        ranked.sort(Comparator.comparingDouble(Ranked::score).reversed());
        return ranked;
    }

    /** How far the best candidate leads the second best; 0 when there are fewer than two. */
    public static double margin(List<Ranked> ranked) {
        return ranked.size() < 2 ? 0 : ranked.get(0).score() - ranked.get(1).score();
    }
}
