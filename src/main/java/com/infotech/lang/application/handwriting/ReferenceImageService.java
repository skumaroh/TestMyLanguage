package com.infotech.lang.application.handwriting;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Finds a letter's reference image: the address in {@code writing_letter}, then the picture in the letter-references container. */
@Service
public class ReferenceImageService implements ReferenceImages {

    private final ReferenceLetterRepository letters;
    private final AssessmentMediaService media;

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
            String url = row.getReferenceBlobUrl();
            if (url == null || url.isBlank()) {
                continue;
            }
            String[] parts = url.trim().split("/", 5);
            if (parts.length < 5 || !AssessmentMediaService.REFERENCE_CONTAINER.equals(parts[3])) {
                continue;
            }
            Optional<byte[]> png = media.read(parts[3], URLDecoder.decode(parts[4], StandardCharsets.UTF_8));
            if (png.isPresent()) {
                return png;
            }
        }
        return Optional.empty();
    }
}
