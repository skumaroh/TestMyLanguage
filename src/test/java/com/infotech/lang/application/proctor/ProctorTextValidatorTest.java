package com.infotech.lang.application.proctor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProctorTextValidatorTest {

    private final ProctorTextValidator validator = new ProctorTextValidator();

    private static final String TAMIL_PHRASE = "வருக வருக"; // short Tamil words
    private static final String HINDI_PHRASE = "स्वागत है"; // short Hindi words
    private static final String ENGLISH = "Welcome, {name}! You will try five short activities today. Take your time.";

    @Test
    void acceptsEnglishPlusTamilPhrase() {
        assertTrue(validator.validate(ENGLISH + "\n" + TAMIL_PHRASE, "tamil").isEmpty());
    }

    @Test
    void acceptsEnglishPlusHindiPhrase() {
        assertTrue(validator.validate(ENGLISH + "\n" + HINDI_PHRASE, "hindi").isEmpty());
    }

    @Test
    void rejectsPhraseInTheWrongScript() {
        List<String> problems = validator.validate(ENGLISH + "\n" + HINDI_PHRASE, "tamil");
        assertFalse(problems.isEmpty());
    }

    @Test
    void rejectsLinksUnknownPlaceholdersAndScoreTalk() {
        assertFalse(validator.validate(ENGLISH + " Visit https://example.com now.\n" + TAMIL_PHRASE, "tamil").isEmpty());
        assertFalse(validator.validate("Welcome, {student}! You will try five short activities today.\n" + TAMIL_PHRASE, "tamil").isEmpty());
        assertFalse(validator.validate("Welcome, {name}! A high score means you passed the placement assessment.\n" + TAMIL_PHRASE, "tamil").isEmpty());
    }

    @Test
    void rejectsEmptyAndTooLongText() {
        assertFalse(validator.validate("", "tamil").isEmpty());
        assertFalse(validator.validate(ENGLISH + " " + "x".repeat(700) + "\n" + TAMIL_PHRASE, "tamil").isEmpty());
    }
}
