package com.infotech.lang.application.handwriting;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Extra reference pictures made from one reference image, so a drawing is matched against the letter written a little thinner and a
 * little bolder as well as as-is. A learner's pen line is rarely the same thickness as the reference. Size and tilt are not varied
 * because the shape comparison already normalises the size of the letter and searches a range of tilts. A picture that cannot be read
 * just has no variants; the original is always the first entry.
 */
public final class ReferenceVariants {

    private static final int MAX_CACHED = 600;
    private static final Map<String, List<byte[]>> CACHE = new ConcurrentHashMap<>();

    private ReferenceVariants() {
    }

    /** The original first, then (when it can be read) a bolder and a thinner copy. */
    public static List<byte[]> of(byte[] png) {
        if (png == null || png.length == 0) {
            return List.of();
        }
        String key = Arrays.hashCode(png) + ":" + png.length;
        List<byte[]> cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        List<byte[]> made = make(png);
        if (CACHE.size() >= MAX_CACHED) {
            CACHE.clear();
        }
        CACHE.put(key, made);
        return made;
    }

    static List<byte[]> make(byte[] png) {
        List<byte[]> out = new ArrayList<>();
        out.add(png);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null) {
                return out;
            }
            boolean[][] ink = inkMask(image);
            int radius = Math.max(1, Math.round(Math.max(image.getWidth(), image.getHeight()) / 100f));
            out.add(toPng(grow(ink, radius), image.getWidth(), image.getHeight()));
            out.add(toPng(shrink(ink, radius), image.getWidth(), image.getHeight()));
        } catch (Exception e) {
            // keep the original only
        }
        return out;
    }

    /** Same ink rule as the shape comparison: not transparent and dark. */
    static boolean[][] inkMask(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int luminance = (((argb >>> 16) & 0xFF) * 299 + ((argb >>> 8) & 0xFF) * 587 + (argb & 0xFF) * 114) / 1000;
                mask[y][x] = alpha >= 32 && luminance < 128;
            }
        }
        return mask;
    }

    static boolean[][] grow(boolean[][] mask, int radius) {
        return morph(mask, radius, true);
    }

    static boolean[][] shrink(boolean[][] mask, int radius) {
        return morph(mask, radius, false);
    }

    /** Square structuring element, done as a horizontal pass and a vertical pass. */
    private static boolean[][] morph(boolean[][] mask, int radius, boolean grow) {
        int h = mask.length;
        int w = h == 0 ? 0 : mask[0].length;
        boolean[][] horizontal = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                horizontal[y][x] = window(mask, y, x, 0, radius, grow);
            }
        }
        boolean[][] result = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                result[y][x] = window(horizontal, y, x, radius, 0, grow);
            }
        }
        return result;
    }

    private static boolean window(boolean[][] m, int y, int x, int dy, int dx, boolean grow) {
        int h = m.length;
        int w = m[0].length;
        for (int j = -dy; j <= dy; j++) {
            for (int i = -dx; i <= dx; i++) {
                int yy = y + j;
                int xx = x + i;
                boolean v = yy >= 0 && yy < h && xx >= 0 && xx < w && m[yy][xx];
                if (grow && v) {
                    return true;
                }
                if (!grow && !v) {
                    return false;
                }
            }
        }
        return !grow;
    }

    private static byte[] toPng(boolean[][] ink, int w, int h) throws java.io.IOException {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int black = Color.BLACK.getRGB();
        int white = Color.WHITE.getRGB();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, ink[y][x] ? black : white);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
