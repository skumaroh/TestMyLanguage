package com.infotech.lang.application.translation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SentencePickerTest {

    @Test
    void acceptsTheCommonWaysATamilCodeIsStored() {
        for (String code : new String[] {"ta", "ta-IN", "ta_IN", "tamil", "Tamil", " TA ", "", null}) {
            assertTrue(SentencePicker.languageMatches(code, "tamil"), String.valueOf(code));
        }
    }

    @Test
    void acceptsTheCommonWaysAHindiCodeIsStored() {
        for (String code : new String[] {"hi", "hi-IN", "hindi", "HI"}) {
            assertTrue(SentencePicker.languageMatches(code, "hindi"), code);
        }
    }

    @Test
    void rejectsAnotherLanguage() {
        assertFalse(SentencePicker.languageMatches("hi", "tamil"));
        assertFalse(SentencePicker.languageMatches("en", "hindi"));
        assertFalse(SentencePicker.languageMatches("ta-IN", "hindi"));
    }
}
