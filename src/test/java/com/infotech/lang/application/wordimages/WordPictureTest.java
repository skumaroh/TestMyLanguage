package com.infotech.lang.application.wordimages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WordPictureTest {

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void aGeneratedPictureIsMadeTheSameSizeAsTheOneItReplaces() throws Exception {
        byte[] fitted = ImageResizer.fit(png(1024, 1024), 600, 400, "png");
        ImageResizer.Size size = ImageResizer.sizeOf(fitted);
        assertEquals(600, size.width());
        assertEquals(400, size.height());
        ImageResizer.Size jpg = ImageResizer.sizeOf(ImageResizer.fit(png(300, 900), 200, 200, "jpg"));
        assertEquals(200, jpg.width());
        assertEquals(200, jpg.height());
    }

    @Test
    void aWebpPictureFromMetaCanBeReadAndResized() throws Exception {
        // a 1x1 lossless WebP
        byte[] webp = java.util.Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");
        ImageResizer.Size size = ImageResizer.sizeOf(webp);
        org.junit.jupiter.api.Assertions.assertNotNull(size, "WebP must be readable");
        ImageResizer.Size fitted = ImageResizer.sizeOf(ImageResizer.fit(webp, 120, 80, "png"));
        assertEquals(120, fitted.width());
    }
}
