package com.infotech.lang.application.data.service;

/**
 * Scores how closely a submitted handwriting sample matches a reference
 * letter image. Implementations are swappable behind this interface so a
 * paid cloud vision service can later replace (or supplement) a local,
 * free comparison without touching the rest of the recognition pipeline.
 */
public interface LetterShapeComparisonService {

    ComparisonResult compare(byte[] submittedPng, byte[] referencePng);

    record ComparisonResult(double similarityScore, boolean matches) {
    }
}
