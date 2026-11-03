package com.infotech.lang.application.handwriting;

/** A letter with the PNG of its reference image: one of the possible answers a drawing is compared with. */
public record LetterCandidate(String letter, byte[] png) {
}
