package com.infotech.lang.application.translation;

/** One sentence to translate: the English prompt and the stored target-language sentence (one correct answer). */
public record TranslationItem(long id, String english, String expected) {
}
