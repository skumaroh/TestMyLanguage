package com.infotech.lang.application.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FeedbackServiceTest {

    private final FeedbackRepository repository = mock(FeedbackRepository.class);
    private final FeedbackService service = new FeedbackService(repository);
    private final UUID user = UUID.randomUUID();

    @Test
    void savesTrimmedFeedbackAndCapsTheText() {
        when(repository.countByUserIdAndCreatedAtAfter(any(), any())).thenReturn(0L);
        FeedbackService.Result r = service.submit(user, "menu", 4, "  Nice  ", "x".repeat(5000));
        assertTrue(r.ok());
        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(repository).save(saved.capture());
        assertEquals("Nice", saved.getValue().getExperience());
        assertEquals(FeedbackService.MAX_CHARS, saved.getValue().getSuggestions().length());
        assertEquals(4, saved.getValue().getStars());
        assertFalse(saved.getValue().isDefect() || saved.getValue().isTicket() || saved.getValue().isAddressed());
    }

    @Test
    void blankTextIsAllowedWithStarsAndStoredAsNull() {
        when(repository.countByUserIdAndCreatedAtAfter(any(), any())).thenReturn(0L);
        assertTrue(service.submit(user, "results", 5, "  ", null).ok());
        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getExperience());
        assertNull(saved.getValue().getSuggestions());
    }

    @Test
    void rejectsBadStarsNoUserAndTooFrequentSends() {
        assertFalse(service.submit(user, "menu", 0, "a", "b").ok());
        assertFalse(service.submit(user, "menu", 6, "a", "b").ok());
        assertFalse(service.submit(null, "menu", 3, "a", "b").ok());
        when(repository.countByUserIdAndCreatedAtAfter(any(), any())).thenReturn(1L);
        FeedbackService.Result r = service.submit(user, "menu", 3, "a", "b");
        assertFalse(r.ok());
        assertTrue(r.message().contains("wait"));
        verify(repository, never()).save(any());
    }

    @Test
    void triageFlagsAreSavedWithWhoAndWhen() {
        Feedback f = new Feedback(user, "menu", 2, "bad", "fix it");
        when(repository.findById(7L)).thenReturn(Optional.of(f));
        UUID admin = UUID.randomUUID();
        assertTrue(service.saveTriage(7L, true, true, "  JIRA-1  ", false, admin));
        assertTrue(f.isDefect());
        assertTrue(f.isTicket());
        assertEquals("JIRA-1", f.getTicketRef());
        assertFalse(f.isAddressed());
        assertEquals(admin, f.getUpdatedBy());
        assertTrue(f.getUpdatedAt().isAfter(OffsetDateTime.now().minusMinutes(1)));
        when(repository.findById(8L)).thenReturn(Optional.empty());
        assertFalse(service.saveTriage(8L, true, false, null, false, admin));
    }
}
