package com.infotech.lang.application.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class InterestServiceTest {

    @Test
    void anInvalidInterestIsNotSaved() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InterestService service = new InterestService(jdbc);
        assertFalse(service.save(UUID.randomUUID(), "tamil", 1, 5L, "Asha", "a@example.com", "SOMETIMES", List.of(), "", "", false));
        assertFalse(service.save(null, "tamil", 1, 5L, "Asha", "a@example.com", "VERY", List.of(), "", "", false));
        verifyNoInteractions(jdbc);
    }

    @Test
    void textIsTrimmedEmptyBecomesNullAndLongTextIsCut() {
        assertNull(InterestService.clip("   ", 10));
        assertNull(InterestService.clip(null, 10));
        assertEquals("abc", InterestService.clip("  abc ", 10));
        assertEquals(5, InterestService.clip("abcdefghij", 5).length());
    }

    @Test
    void topicsRoundTripThroughTheirStoredForm() {
        assertEquals(List.of("Grammar", "Another language"), InterestService.topicList("Grammar, Another language,"));
        assertTrue(InterestService.topicList(null).isEmpty());
    }
}
