package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Finds a letter's reference image: the address in {@code writing_letter}, then the picture in the letter-references container. */
@Service
public class ReferenceImageService implements ReferenceImages {

    private final ReferenceLetterRepository letters;
    private final AssessmentMediaService media;
    /** Reference pictures are small and rarely change: kept in memory once read. */
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public ReferenceImageService(ReferenceLetterRepository letters, AssessmentMediaService media) {
        this.letters = letters;
        this.media = media;
    }

    @Override
    public Optional<byte[]> pngFor(String letter) {
        if (letter == null || letter.isBlank()) {
            return Optional.empty();
        }
        for (ReferenceLetter row : letters.findForLetter(letter.trim())) {
            Optional<byte[]> png = pngOf(row);
            if (png.isPresent()) {
                return png;
            }
        }
        return Optional.empty();
    }

    @Override
    public List<LetterCandidate> candidatesFor(String expectedLetter, int max) {
        if (expectedLetter == null || expectedLetter.isBlank() || max < 1) {
            return List.of();
        }
        List<ReferenceLetter> own = letters.findForLetter(expectedLetter.trim());
        if (own.isEmpty()) {
            return List.of();
        }
        ReferenceLetter first = own.get(0);
        List<ReferenceLetter> group = first.getCategory() == null ? List.of()
                : first.getSubCategory() == null || first.getSubCategory().isBlank()
                        ? letters.findHouseByCategory(first.getCategory())
                        : letters.findHouseByCategoryAndSub(first.getCategory(), first.getSubCategory());
        Map<String, byte[]> found = new LinkedHashMap<>();
        pngFor(expectedLetter).ifPresent(png -> found.put(expectedLetter.trim(), png));
        for (ReferenceLetter row : group) {
            if (found.size() >= max) {
                break;
            }
            String letter = row.getLetter() == null ? "" : row.getLetter().trim();
            if (letter.isEmpty() || found.containsKey(letter)) {
                continue;
            }
            pngOf(row).ifPresent(png -> found.put(letter, png));
        }
        List<LetterCandidate> result = new ArrayList<>();
        found.forEach((letter, png) -> result.add(new LetterCandidate(letter, png)));
        return result;
    }

    private Optional<byte[]> pngOf(ReferenceLetter row) {
        String url = row.getReferenceBlobUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String[] parts = url.trim().split("/", 5);
        if (parts.length < 5 || !AssessmentMediaService.REFERENCE_CONTAINER.equals(parts[3])) {
            return Optional.empty();
        }
        String blob = URLDecoder.decode(parts[4], StandardCharsets.UTF_8);
        byte[] cached = cache.get(blob);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<byte[]> png = media.read(parts[3], blob);
        png.ifPresent(bytes -> cache.put(blob, bytes));
        return png;
    }
}
