package com.infotech.lang.application.views.api;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves word pictures and audio from Blob Storage to signed-in students ({@code SecurityConfig} requires authentication).
 * <pre>GET /api/media/audio/{container}/{blob path}      GET /api/media/image/{container}/{blob path}</pre>
 * Only the two known containers are served, and byte ranges are supported because Safari needs them to play audio.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final AssessmentMediaService media;

    public MediaController(AssessmentMediaService media) {
        this.media = media;
    }

    @GetMapping("/{kind}/**")
    public ResponseEntity<byte[]> get(@PathVariable String kind,
                                      HttpServletRequest request,
                                      @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        String prefix = request.getContextPath() + "/api/media/" + kind + "/";
        String uri = request.getRequestURI();
        if (!uri.startsWith(prefix)) {
            return ResponseEntity.badRequest().build();
        }
        String rest = URLDecoder.decode(uri.substring(prefix.length()), StandardCharsets.UTF_8);
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) {
            return ResponseEntity.badRequest().build();
        }
        String container = rest.substring(0, slash);
        String blob = rest.substring(slash + 1);
        if (!AssessmentMediaService.isAllowed(kind, container) || blob.startsWith("/")
                || Arrays.asList(blob.split("/")).contains("..")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<byte[]> data = media.read(container, blob);
        if (data.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        byte[] bytes = data.get();
        MediaType type = contentType(blob);
        CacheControl cache = CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate();
        long[] window = parseRange(range, bytes.length);
        if (window == null) {
            return ResponseEntity.ok().contentType(type).cacheControl(cache)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes").body(bytes);
        }
        if (window[0] < 0) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + bytes.length).build();
        }
        byte[] slice = Arrays.copyOfRange(bytes, (int) window[0], (int) window[1] + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(type).cacheControl(cache)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + window[0] + "-" + window[1] + "/" + bytes.length)
                .body(slice);
    }

    /** Null when there is no usable Range header; {-1, -1} when it cannot be satisfied; else {first, last} byte, inclusive. */
    static long[] parseRange(String header, int length) {
        if (header == null || !header.startsWith("bytes=") || length == 0) {
            return null;
        }
        String spec = header.substring("bytes=".length()).split(",")[0].trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return null;
        }
        try {
            String from = spec.substring(0, dash).trim();
            String to = spec.substring(dash + 1).trim();
            long first;
            long last;
            if (from.isEmpty()) {
                long suffix = Long.parseLong(to);
                if (suffix <= 0) {
                    return new long[] {-1, -1};
                }
                first = Math.max(0, length - suffix);
                last = length - 1L;
            } else {
                first = Long.parseLong(from);
                last = to.isEmpty() ? length - 1L : Math.min(Long.parseLong(to), length - 1L);
            }
            if (first > last || first >= length) {
                return new long[] {-1, -1};
            }
            return new long[] {first, last};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static MediaType contentType(String blob) {
        String name = blob.toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3")) {
            return MediaType.parseMediaType("audio/mpeg");
        }
        if (name.endsWith(".wav")) {
            return MediaType.parseMediaType("audio/wav");
        }
        if (name.endsWith(".m4a") || name.endsWith(".mp4")) {
            return MediaType.parseMediaType("audio/mp4");
        }
        if (name.endsWith(".ogg")) {
            return MediaType.parseMediaType("audio/ogg");
        }
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
