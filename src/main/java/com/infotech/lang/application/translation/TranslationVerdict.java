package com.infotech.lang.application.translation;

/** What the grader decided about a student's translation. Only {@link #SAME_MEANING} counts as correct. */
public enum TranslationVerdict {
    /** The answer is in the language and says the same thing, even if worded differently from the reference. */
    SAME_MEANING,
    /** The answer is in the language but says something else, or misses part of the meaning. */
    DIFFERENT_MEANING,
    /** The answer is mostly in another language or script (for example English letters). */
    NOT_IN_LANGUAGE,
    /** Empty, nonsense or unreadable. */
    UNREADABLE,
    /** The model could not be asked or its answer could not be used; never counts as correct. */
    ERROR
}
