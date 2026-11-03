package com.infotech.lang.application.data.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Compares a submitted handwriting PNG against a reference letter PNG using
 * classical image processing: binarize both images, crop each to its own ink
 * bounding box, normalize onto a fixed-size canvas (preserving aspect ratio),
 * then score similarity from how close the two ink strokes are to each other
 * (symmetric Chamfer distance), searching a small range of rotations of the
 * submitted strokes to tolerate natural handwriting tilt.
 *
 * Runs entirely in-process with the JDK's own image classes - no external
 * service call, no native dependency, no per-comparison cost.
 */
@Service
public class LocalImageShapeComparisonService implements LetterShapeComparisonService {

    private static final int CANVAS_SIZE = 64;
    private static final int INK_LUMINANCE_THRESHOLD = 128;
    private static final int TRANSPARENT_ALPHA_THRESHOLD = 32;

    /** Bounded rotation search applied to the submitted canvas, to tolerate natural tilt. */
    private static final double ROTATION_SEARCH_RANGE_DEGREES = 20.0;
    private static final double ROTATION_SEARCH_STEP_DEGREES = 4.0;

    /** Chamfer 3-4 distance transform step costs, scaled by 1/3 to approximate Euclidean distance. */
    private static final int CHAMFER_ORTHOGONAL_STEP = 3;
    private static final int CHAMFER_DIAGONAL_STEP = 4;
    private static final double CHAMFER_EUCLIDEAN_SCALE = 3.0;
    private static final double NO_INK_DISTANCE = CANVAS_SIZE * 2.0;

    @Value("${handwriting.shape-match.threshold:0.5}")
    private double matchThreshold;

    /**
     * Controls how quickly similarity falls off with average stroke distance (in canvas
     * pixels). Larger sigma = more forgiving of misalignment/stroke-thickness differences.
     * Tune via HANDWRITING_SHAPE_MATCH_SIGMA against real attempts in the admin panel
     * ("Re-run recognition" + "Refresh") - the right value depends on actual handwriting
     * samples that aren't available outside the running app.
     */
    @Value("${handwriting.shape-match.sigma:10.0}")
    private double sigma;

    @Override
    public ComparisonResult compare(byte[] submittedPng, byte[] referencePng) {
        boolean[][] submittedCanvas = normalize(submittedPng);
        boolean[][] referenceCanvas = normalize(referencePng);
        double[][] referenceDistanceTransform = distanceTransform(referenceCanvas);

        double bestSimilarity = 0.0;
        for (double angle = -ROTATION_SEARCH_RANGE_DEGREES; angle <= ROTATION_SEARCH_RANGE_DEGREES;
                angle += ROTATION_SEARCH_STEP_DEGREES) {
            boolean[][] rotatedSubmitted = rotate(submittedCanvas, Math.toRadians(angle));
            double[][] submittedDistanceTransform = distanceTransform(rotatedSubmitted);
            double avgDistance = (averageDistance(rotatedSubmitted, referenceDistanceTransform)
                    + averageDistance(referenceCanvas, submittedDistanceTransform)) / 2.0;
            bestSimilarity = Math.max(bestSimilarity, Math.exp(-avgDistance / sigma));
        }

        return new ComparisonResult(bestSimilarity, bestSimilarity >= matchThreshold);
    }

    private boolean[][] normalize(byte[] pngBytes) {
        boolean[][] inkMask = toInkMask(decode(pngBytes));
        int[] bbox = boundingBox(inkMask);
        return toCanvas(inkMask, bbox);
    }

    private BufferedImage decode(byte[] pngBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                throw new IllegalArgumentException("Unable to decode image for shape comparison.");
            }
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to decode image for shape comparison.", e);
        }
    }

    private boolean[][] toInkMask(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mask[y][x] = isInk(image.getRGB(x, y));
            }
        }
        return mask;
    }

    private boolean isInk(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < TRANSPARENT_ALPHA_THRESHOLD) {
            return false;
        }
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance < INK_LUMINANCE_THRESHOLD;
    }

    /** Returns {minX, minY, maxX, maxY} inclusive, or null if the mask has no ink. */
    private int[] boundingBox(boolean[][] mask) {
        int height = mask.length;
        int width = height == 0 ? 0 : mask[0].length;
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y][x]) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[] { minX, minY, maxX, maxY };
    }

    private boolean[][] toCanvas(boolean[][] mask, int[] bbox) {
        boolean[][] canvas = new boolean[CANVAS_SIZE][CANVAS_SIZE];
        if (bbox == null) {
            return canvas;
        }

        int minX = bbox[0], minY = bbox[1], maxX = bbox[2], maxY = bbox[3];
        int boxWidth = maxX - minX + 1;
        int boxHeight = maxY - minY + 1;
        double scale = Math.min((double) CANVAS_SIZE / boxWidth, (double) CANVAS_SIZE / boxHeight);
        int scaledWidth = Math.max(1, (int) Math.round(boxWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(boxHeight * scale));
        int offsetX = (CANVAS_SIZE - scaledWidth) / 2;
        int offsetY = (CANVAS_SIZE - scaledHeight) / 2;

        for (int cy = 0; cy < scaledHeight; cy++) {
            int srcY = minY + Math.min(boxHeight - 1, (int) (cy / scale));
            for (int cx = 0; cx < scaledWidth; cx++) {
                int srcX = minX + Math.min(boxWidth - 1, (int) (cx / scale));
                canvas[offsetY + cy][offsetX + cx] = mask[srcY][srcX];
            }
        }
        return canvas;
    }

    /** Rotates a canvas mask about its center by the given angle, using inverse nearest-neighbor sampling. */
    private boolean[][] rotate(boolean[][] mask, double angleRadians) {
        if (angleRadians == 0.0) {
            return mask;
        }
        boolean[][] rotated = new boolean[CANVAS_SIZE][CANVAS_SIZE];
        double center = (CANVAS_SIZE - 1) / 2.0;
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        for (int y = 0; y < CANVAS_SIZE; y++) {
            double dy = y - center;
            for (int x = 0; x < CANVAS_SIZE; x++) {
                double dx = x - center;
                int srcX = (int) Math.round(center + dx * cos + dy * sin);
                int srcY = (int) Math.round(center - dx * sin + dy * cos);
                if (srcX >= 0 && srcX < CANVAS_SIZE && srcY >= 0 && srcY < CANVAS_SIZE) {
                    rotated[y][x] = mask[srcY][srcX];
                }
            }
        }
        return rotated;
    }

    /**
     * Chamfer 3-4 distance transform: for every cell, the approximate distance (in pixels)
     * to the nearest ink cell in the mask. Cheap two-pass alternative to an exact Euclidean
     * distance transform, accurate to within a few percent.
     */
    private double[][] distanceTransform(boolean[][] mask) {
        double[][] distance = new double[CANVAS_SIZE][CANVAS_SIZE];
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                distance[y][x] = mask[y][x] ? 0 : Double.MAX_VALUE;
            }
        }
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                if (x > 0) {
                    distance[y][x] = Math.min(distance[y][x], distance[y][x - 1] + CHAMFER_ORTHOGONAL_STEP);
                }
                if (y > 0) {
                    distance[y][x] = Math.min(distance[y][x], distance[y - 1][x] + CHAMFER_ORTHOGONAL_STEP);
                    if (x > 0) {
                        distance[y][x] = Math.min(distance[y][x], distance[y - 1][x - 1] + CHAMFER_DIAGONAL_STEP);
                    }
                    if (x < CANVAS_SIZE - 1) {
                        distance[y][x] = Math.min(distance[y][x], distance[y - 1][x + 1] + CHAMFER_DIAGONAL_STEP);
                    }
                }
            }
        }
        for (int y = CANVAS_SIZE - 1; y >= 0; y--) {
            for (int x = CANVAS_SIZE - 1; x >= 0; x--) {
                if (x < CANVAS_SIZE - 1) {
                    distance[y][x] = Math.min(distance[y][x], distance[y][x + 1] + CHAMFER_ORTHOGONAL_STEP);
                }
                if (y < CANVAS_SIZE - 1) {
                    distance[y][x] = Math.min(distance[y][x], distance[y + 1][x] + CHAMFER_ORTHOGONAL_STEP);
                    if (x < CANVAS_SIZE - 1) {
                        distance[y][x] = Math.min(distance[y][x], distance[y + 1][x + 1] + CHAMFER_DIAGONAL_STEP);
                    }
                    if (x > 0) {
                        distance[y][x] = Math.min(distance[y][x], distance[y + 1][x - 1] + CHAMFER_DIAGONAL_STEP);
                    }
                }
            }
        }
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                distance[y][x] /= CHAMFER_EUCLIDEAN_SCALE;
            }
        }
        return distance;
    }

    /** Mean distance, in pixels, from each ink cell of {@code mask} to the nearest ink cell of the other shape. */
    private double averageDistance(boolean[][] mask, double[][] otherDistanceTransform) {
        double sum = 0.0;
        int count = 0;
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                if (mask[y][x]) {
                    sum += otherDistanceTransform[y][x];
                    count++;
                }
            }
        }
        return count == 0 ? NO_INK_DISTANCE : sum / count;
    }
}
