package com.infotech.lang.application.handwriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.data.service.LetterShapeComparisonService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ReferenceVariantsTest {

    /** A 100 x 100 white picture with a black vertical bar 6 pixels wide. */
    private static byte[] bar() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                image.setRGB(x, y, x >= 47 && x < 53 && y >= 10 && y < 90 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static long inkOf(byte[] png) throws Exception {
        boolean[][] mask = ReferenceVariants.inkMask(ImageIO.read(new ByteArrayInputStream(png)));
        long n = 0;
        for (boolean[] row : mask) {
            for (boolean v : row) {
                n += v ? 1 : 0;
            }
        }
        return n;
    }

    @Test
    void theOriginalComesFirstThenABolderAndAThinnerCopy() throws Exception {
        byte[] original = bar();
        List<byte[]> variants = ReferenceVariants.of(original);
        assertEquals(3, variants.size());
        org.junit.jupiter.api.Assertions.assertArrayEquals(original, variants.get(0));
        long normal = inkOf(variants.get(0));
        assertTrue(inkOf(variants.get(1)) > normal, "bolder has more ink");
        assertTrue(inkOf(variants.get(2)) < normal, "thinner has less ink");
        assertTrue(inkOf(variants.get(2)) > 0, "thinner still has some ink");
    }

    @Test
    void anUnreadablePictureJustKeepsItself() {
        byte[] junk = {1, 2, 3};
        assertEquals(1, ReferenceVariants.of(junk).size());
        assertEquals(0, ReferenceVariants.of(new byte[0]).size());
        assertEquals(0, ReferenceVariants.of(null).size());
    }

    @Test
    void theBestOfTheVersionsIsTheCandidatesScore() throws Exception {
        byte[] original = bar();
        List<byte[]> variants = ReferenceVariants.of(original);
        // a fake comparison that likes only the bolder copy
        LetterShapeComparisonService likesBold = (drawn, ref) ->
                new LetterShapeComparisonService.ComparisonResult(ref == variants.get(1) ? 0.9 : 0.5, true);
        List<ShapeRanking.Ranked> plain = ShapeRanking.rank(likesBold, new byte[] {1}, List.of(new LetterCandidate("ப", original)), false);
        List<ShapeRanking.Ranked> varied = ShapeRanking.rank(likesBold, new byte[] {1}, List.of(new LetterCandidate("ப", original)), true);
        assertEquals(0.5, plain.get(0).score(), 1e-9);
        assertEquals(0.9, varied.get(0).score(), 1e-9);
    }
}
