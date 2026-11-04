package com.infotech.lang.application.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiResult;
import org.junit.jupiter.api.Test;

class CertificateNameTest {

    @Test
    void ordinaryNamesInAnyScriptAreAccepted() {
        for (String n : new String[] {"Asha Rao", "அஷா குமார்", "आशा राव", "Mary-Jane O'Neil", "S. K. Rao", "Zoë Müller"}) {
            assertNull(CertificateName.problem(CertificateName.clean(n)), n);
        }
    }

    @Test
    void emptyShortLongAndOddNamesAreRefused() {
        assertNotNull(CertificateName.problem(CertificateName.clean("   ")));
        assertNotNull(CertificateName.problem(CertificateName.clean(null)));
        assertNotNull(CertificateName.problem(CertificateName.clean("A")));
        assertNotNull(CertificateName.problem(CertificateName.clean("x".repeat(101))));
        assertNotNull(CertificateName.problem(CertificateName.clean("Asha 123")));
        assertNotNull(CertificateName.problem(CertificateName.clean("www.example.com")));
        assertNotNull(CertificateName.problem(CertificateName.clean("asha@example.com")));
        assertNotNull(CertificateName.problem(CertificateName.clean("Asha 😀")));
        assertNotNull(CertificateName.problem(CertificateName.clean("-Asha")));
    }

    @Test
    void cleaningSqueezesSpacesAndRemovesControlCharacters() {
        assertEquals("Asha Rao", CertificateName.clean("  Asha \t\n  Rao  "));
        assertEquals("Asha Rao", CertificateName.clean("Asha\u0000Rao"));
        assertEquals(CertificateName.MAX + 1, CertificateName.clean("y".repeat(500)).length());
    }

    @Test
    void theCodeLooksLikeThreeGroupsOfFourWithoutAmbiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            String code = CertificateAudit.newCode();
            assertTrue(code.matches("[A-HJKMNP-Z2-9]{4}-[A-HJKMNP-Z2-9]{4}-[A-HJKMNP-Z2-9]{4}"), code);
        }
        assertTrue(!CertificateAudit.newCode().equals(CertificateAudit.newCode()));
    }

    @Test
    void theScreenTreatsTheNameAsDataAndFailsOpenWhenTheModelIsDown() {
        assertTrue(NameScreen.task("Asha Rao").contains("<<<NAME\nAsha Rao\nNAME>>>"));
        assertTrue(new NameScreen(r -> AiResult.success("{\"acceptable\":false,\"reason\":\"joke\"}", 10, 1, 1, 1))
                .check("Mickey Mouse").acceptable() == false);
        NameScreen.Result down = new NameScreen(r -> AiResult.failure(500, 5, "down")).check("Asha Rao");
        assertTrue(down.acceptable());
        assertTrue(!down.checked());
        NameScreen.Result junk = new NameScreen(r -> AiResult.success("not json", 10, 1, 1, 1)).check("Asha Rao");
        assertTrue(junk.acceptable() && !junk.checked());
    }
}
