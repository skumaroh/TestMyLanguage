package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.text.Normalizer;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Judges a drawing by two methods and combines them (owner decision, 2026-11-01): the old app's shape comparison against the
 * letter's reference image (weighs more), and the Meta vision model reading the drawing blind. Java decides; the model never
 * says correct or wrong. The drawing passes when the weighted result reaches the threshold and Meta could be asked.
 */
@Service
public class CombinedHandwritingCheck {

    private static final Logger logger = LoggerFactory.getLogger(CombinedHandwritingCheck.class);

    /**
     * @param localScore       the shape comparison, 0 to 1, or null when the letter has no usable reference image
     * @param metaRead         the character Meta read ("" when it could not read one)
     * @param metaConfidence   Meta's own confidence, 0 to 1
     * @param metaScore        Meta's part: its confidence when it read the expected letter, otherwise 0
     * @param combined         the weighted result, 0 to 1, or null when there is no local score
     */
    public record Result(Double localScore, boolean metaAnswered, String metaRead, double metaConfidence,
                         double metaScore, Double combined, boolean passed, String note, long metaMs) {
    }

    private final LetterShapeComparisonService shape;
    private final HandwritingReader reader;
    private final ReferenceImages references;
    private final double weightLocal;
    private final double weightMeta;
    private final double threshold;

    public CombinedHandwritingCheck(LetterShapeComparisonService shape,
                                    HandwritingReader reader,
                                    ReferenceImages references,
                                    @Value("${handwriting.weight.local:0.7}") double weightLocal,
                                    @Value("${handwriting.weight.meta:0.3}") double weightMeta,
                                    @Value("${handwriting.pass-threshold:0.8}") double threshold) {
        this.shape = shape;
        this.reader = reader;
        this.references = references;
        this.weightLocal = weightLocal;
        this.weightMeta = weightMeta;
        this.threshold = threshold;
    }

    public Result check(byte[] drawingPng, String language, String expectedLetter) {
        Double local = localScore(drawingPng, expectedLetter);
        HandwritingReader.Reading reading = reader.read(drawingPng, language);
        double metaScore = reading.ok() && sameLetter(reading.character(), expectedLetter) ? reading.confidence() : 0;
        Double combined = local == null ? null : combine(local, metaScore, weightLocal, weightMeta);
        String note;
        boolean passed = false;
        if (local == null) {
            note = "No reference image for this letter, so the shape comparison could not run.";
        } else if (!reading.ok()) {
            note = "Meta did not answer, so the drawing cannot pass this time.";
        } else {
            passed = combined >= threshold;
            note = passed ? "Passed." : "Below the pass threshold.";
        }
        return new Result(local, reading.ok(), reading.character(), reading.confidence(), metaScore, combined, passed,
                note, reading.latencyMs());
    }

    private Double localScore(byte[] drawingPng, String expectedLetter) {
        Optional<byte[]> reference = references.pngFor(expectedLetter);
        if (reference.isEmpty()) {
            return null;
        }
        try {
            return shape.compare(drawingPng, reference.get()).similarityScore();
        } catch (RuntimeException e) {
            logger.warn("Shape comparison failed: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    /** The weighted average of the two scores; the weights are scaled so they add up to 1. */
    static double combine(double local, double meta, double weightLocal, double weightMeta) {
        double total = weightLocal + weightMeta;
        if (total <= 0) {
            return 0;
        }
        return (weightLocal * local + weightMeta * meta) / total;
    }

    /** True when both texts are the same letter, ignoring spaces and Unicode composition differences. */
    static boolean sameLetter(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return Normalizer.normalize(a.trim(), Normalizer.Form.NFC).equals(Normalizer.normalize(b.trim(), Normalizer.Form.NFC));
    }
}
