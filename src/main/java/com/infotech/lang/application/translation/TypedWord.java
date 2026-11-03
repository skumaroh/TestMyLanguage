package com.infotech.lang.application.translation;

/** One word the learner typed in English letters: those letters, the script text now shown, and where it sits in the box. */
public record TypedWord(String raw, String shown, int start, int end) {
}
