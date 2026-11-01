package com.infotech.lang.application.data.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LocalImageShapeComparisonServiceTest {

    private LocalImageShapeComparisonService service;

    @BeforeEach
    void setUp() {
        service = new LocalImageShapeComparisonService();
        ReflectionTestUtils.setField(service, "matchThreshold", 0.75);
        ReflectionTestUtils.setField(service, "sigma", 10.0);
    }

    @Test
    void identicalShapesScoreMaximumSimilarity() {
        byte[] square = squarePng(64, 10, 10, 30, 30);

        LetterShapeComparisonService.ComparisonResult result = service.compare(square, square);

        assertEquals(1.0, result.similarityScore(), 0.0001);
        assertTrue(result.matches());
    }

    @Test
    void differentShapesScoreLowSimilarity() {
        byte[] square = squarePng(64, 5, 5, 20, 20);
        byte[] thinLine = squarePng(64, 5, 30, 60, 32);

        LetterShapeComparisonService.ComparisonResult result = service.compare(square, thinLine);

        assertTrue(result.similarityScore() < 0.5,
                "expected low similarity for dissimilar shapes, got " + result.similarityScore());
        assertFalse(result.matches());
    }

    @Test
    void shapeScaledAndTranslatedStillMatchesAfterNormalization() {
        byte[] smallSquareTopLeft = squarePng(64, 2, 2, 12, 12);
        byte[] largeSquareBottomRight = squarePng(64, 40, 40, 60, 60);

        LetterShapeComparisonService.ComparisonResult result =
                service.compare(smallSquareTopLeft, largeSquareBottomRight);

        assertEquals(1.0, result.similarityScore(), 0.0001);
        assertTrue(result.matches());
    }

    @Test
    void slightlyRotatedStrokeStillScoresHigh() {
        byte[] reference = strokePng(80, 15, 15, 65, 65, 4f, 0);
        byte[] rotatedSubmission = strokePng(80, 15, 15, 65, 65, 4f, 12);

        LetterShapeComparisonService.ComparisonResult result = service.compare(rotatedSubmission, reference);

        assertTrue(result.similarityScore() > 0.7,
                "expected a tilted but otherwise identical stroke to score high, got " + result.similarityScore());
    }

    @Test
    void thickerStrokeOfSameShapeStillScoresHigh() {
        byte[] reference = strokePng(80, 15, 15, 65, 65, 2f, 0);
        byte[] thickerSubmission = strokePng(80, 15, 15, 65, 65, 8f, 0);

        LetterShapeComparisonService.ComparisonResult result = service.compare(thickerSubmission, reference);

        assertTrue(result.similarityScore() > 0.6,
                "expected the same stroke path drawn thicker to still score high, got " + result.similarityScore());
    }

    /** Draws a single straight stroke (optionally rotated about the canvas center) and PNG-encodes it. */
    private byte[] strokePng(int canvasSize, int x1, int y1, int x2, int y2, float strokeWidth,
            double rotationDegrees) {
        BufferedImage image = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvasSize, canvasSize);
        graphics.rotate(Math.toRadians(rotationDegrees), canvasSize / 2.0, canvasSize / 2.0);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new java.awt.BasicStroke(strokeWidth));
        graphics.drawLine(x1, y1, x2, y2);
        graphics.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Draws a single filled black square on a white canvas and PNG-encodes it. */
    private byte[] squarePng(int canvasSize, int x1, int y1, int x2, int y2) {
        BufferedImage image = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvasSize, canvasSize);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(x1, y1, x2 - x1, y2 - y1);
        graphics.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
