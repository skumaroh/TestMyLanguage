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
        List<Ranked> ranked = new ArrayList<>();
        for (LetterCandidate candidate : candidates) {
            double score;
            try {
                score = shape.compare(drawingPng, candidate.png()).similarityScore();
            } catch (RuntimeException e) {
                score = 0;
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
