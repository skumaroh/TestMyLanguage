package com.infotech.lang.application.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.infotech.lang.application.assessment.Modules;
import org.junit.jupiter.api.Test;

class HelpImagesTest {

    @Test
    void pathIsPerLanguageAndModule() {
        assertEquals("images/help/tamil/module4.jpg", HelpImages.path(Modules.TRANSLATION, "tamil"));
        assertNull(HelpImages.path(Modules.TRANSLATION, "../x"));
        assertNull(HelpImages.path(Modules.TRANSLATION, null));
    }

    @Test
    void showsTheScreenshotWhenItExistsAndNothingWhenItDoesNot() {
        assertNotNull(HelpImages.forModule(Modules.WORDS, "tamil"));
        assertNull(HelpImages.forModule(Modules.WORDS, "klingon"));
    }
}
