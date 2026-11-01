package com.infotech.lang.application.handwriting;

import java.util.Optional;

/** Where the reference picture of a letter comes from (a seam so the check can be tested without Blob Storage). */
public interface ReferenceImages {

    /** The PNG of the correctly written letter, if an admin uploaded one and it can be read. */
    Optional<byte[]> pngFor(String letter);
}
