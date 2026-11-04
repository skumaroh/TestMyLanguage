package com.infotech.lang.application.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VerifyTest {

    @Test
    void codesAreNormalisedAndOnlyRealLookingCodesPass() {
        assertEquals("K7QM-2XH9-WD4P", CertificateAudit.normalizeCode("k7qm-2xh9-wd4p"));
        assertEquals("K7QM-2XH9-WD4P", CertificateAudit.normalizeCode("  K7QM 2XH9 WD4P "));
        assertEquals("K7QM-2XH9-WD4P", CertificateAudit.normalizeCode("K7QM2XH9WD4P"));
        assertNull(CertificateAudit.normalizeCode("K7QM-2XH9"));            // too short
        assertNull(CertificateAudit.normalizeCode("K7QM-2XH9-WD4P-AAAA"));  // too long
        assertNull(CertificateAudit.normalizeCode("K7QM-2XH9-WD40"));       // 0 is never used
        assertNull(CertificateAudit.normalizeCode("' or 1=1 --"));
        assertNull(CertificateAudit.normalizeCode(null));
        for (int i = 0; i < 100; i++) {
            String code = CertificateAudit.newCode();
            assertEquals(code, CertificateAudit.normalizeCode(code));
        }
    }

    @Test
    void aVisitorIsLimitedToThirtyChecksInTenMinutesAndOthersAreNotAffected() {
        VerifyThrottle throttle = new VerifyThrottle();
        long now = 1_000_000L;
        for (int i = 0; i < VerifyThrottle.MAX_CHECKS; i++) {
            assertTrue(throttle.allow("1.2.3.4", now + i));
        }
        assertFalse(throttle.allow("1.2.3.4", now + 100));
        assertTrue(throttle.allow("5.6.7.8", now + 100));
        assertTrue(throttle.allow("1.2.3.4", now + VerifyThrottle.WINDOW_MINUTES * 60_000L + 1_000));
    }
}
