package com.infotech.lang.application.assessment;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Turns a saved blob URL ({@code https://<account>.blob.core.windows.net/<container>/<blob path>}) into the address of our
 * own media proxy ({@code /api/media/<kind>/<container>/<blob path>}); browsers cannot read the private containers directly.
 */
public final class MediaUrls {

    private MediaUrls() {
    }

    /** {@code kind} is {@code audio} or {@code image}. Returns null for a blank or malformed URL. */
    public static String proxyUrl(String kind, String savedBlobUrl) {
        if (savedBlobUrl == null || savedBlobUrl.isBlank()) {
            return null;
        }
        String[] parts = savedBlobUrl.trim().split("/", 5);
        if (parts.length < 5 || parts[3].isBlank() || parts[4].isBlank()) {
            return null;
        }
        String blobPath = URLDecoder.decode(parts[4], StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder();
        for (String segment : blobPath.split("/")) {
            if (encoded.length() > 0) {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return "/api/media/" + kind + "/" + parts[3] + "/" + encoded;
    }
}
