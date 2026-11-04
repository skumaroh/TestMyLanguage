package com.infotech.lang.application.wordimages;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Makes a generated picture the same size as the one it replaces: centre-crops to the same shape, then scales. */
public final class ImageResizer {

    private ImageResizer() {
    }

    public record Size(int width, int height) {
    }

    /** The picture's width and height, or null when the bytes are not an image Java can read. */
    public static Size sizeOf(byte[] image) {
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
            return decoded == null ? null : new Size(decoded.getWidth(), decoded.getHeight());
        } catch (IOException e) {
            return null;
        }
    }

    /** {@code ext} is {@code png} or {@code jpg}; a jpg has no transparency, so it is drawn on white. */
    public static byte[] fit(byte[] source, int width, int height, String ext) throws IOException {
        BufferedImage in = ImageIO.read(new ByteArrayInputStream(source));
        if (in == null) {
            throw new IOException("not an image");
        }
        double targetRatio = (double) width / height;
        int cropW = in.getWidth();
        int cropH = in.getHeight();
        if ((double) cropW / cropH > targetRatio) {
            cropW = (int) Math.round(cropH * targetRatio);
        } else {
            cropH = (int) Math.round(cropW / targetRatio);
        }
        int x = (in.getWidth() - cropW) / 2;
        int y = (in.getHeight() - cropH) / 2;
        boolean jpg = "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext);
        BufferedImage out = new BufferedImage(width, height, jpg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (jpg) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
            g.drawImage(in, 0, 0, width, height, x, y, x + cropW, y + cropH, null);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(out, jpg ? "jpg" : "png", bytes)) {
            throw new IOException("cannot write " + ext);
        }
        return bytes.toByteArray();
    }
}
