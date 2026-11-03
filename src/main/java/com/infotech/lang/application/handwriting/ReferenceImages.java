package com.infotech.lang.application.handwriting;

import java.util.List;
import java.util.Optional;

/** Where the reference pictures of letters come from (a seam so the checks can be tested without Blob Storage). */
public interface ReferenceImages {

    /** The PNG of the correctly written letter, if an admin uploaded one and it can be read. */
    Optional<byte[]> pngFor(String letter);

    /**
     * The expected letter and the other letters of its group (for example the other vowels) with their reference images,
     * at most {@code max}; the expected letter is always included when it has an image.
     */
    default List<LetterCandidate> candidatesFor(String expectedLetter, int max) {
        return List.of();
    }
}
