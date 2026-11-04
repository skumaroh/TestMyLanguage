package com.infotech.lang.application.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HelpButtonTest {

    @Test
    void onlyPlainEnglishInstructionsGetAListenButton() {
        assertTrue(HelpButton.speakable("Look at the picture and listen to the word if you like."));
        assertTrue(HelpButton.speakable("Press Send for each reply. \"Show English\" helps if you are stuck."));
        assertFalse(HelpButton.speakable("Tip for typing: write Tamil words in English letters (for example \"naan thanneer kudikkiren\")."));
        assertFalse(HelpButton.speakable("Use \"ae\" for ஏ."));
        assertFalse(HelpButton.speakable(""));
        assertFalse(HelpButton.speakable(null));
    }
}
