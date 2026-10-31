package com.infotech.lang.application.proctor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiResult;
import com.infotech.lang.application.config.LanguageContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Uses a fake gateway: no real Meta key or network is needed. */
class ProctorTextServiceTest {

    private static final String TAMIL_PHRASE = "வருக வருக";
    private static final String GOOD_JSON =
            "{\"english\":\"Welcome, {name}! You will try five short activities today. Take your time.\","
                    + "\"phrase\":\"" + TAMIL_PHRASE + "\"}";

    private ProctorTextRepository repository;
    private AiGateway gateway;
    private ProctorTextService service;
    private final ProctorSlot greeting = ProctorSlot.greeting(1, "en");

    @BeforeEach
    void setUp() {
        repository = mock(ProctorTextRepository.class);
        gateway = mock(AiGateway.class);
        LanguageContext languageContext = mock(LanguageContext.class);
        when(languageContext.getLanguage()).thenReturn("tamil");
        service = new ProctorTextService(repository, gateway, new ProctorTextValidator(), languageContext,
                "muse-spark-1.3", "en");
        when(repository.findBySlot(any(), any(), anyInt(), anyString(), any())).thenReturn(List.of());
    }

    @Test
    void resolveReturnsTheFallbackWithTheNameWhenNothingIsApproved() {
        ProctorTextService.ResolvedText resolved = service.resolve(greeting, "Asha");
        assertFalse(resolved.fromMeta());
        assertTrue(resolved.text().contains("Asha"));
        verify(gateway, never()).completeJson(any());
    }

    @Test
    void resolveReturnsTheApprovedMetaTextAndFillsTheName() {
        ProctorText approved = new ProctorText(greeting, "Hello, {name}!\n" + TAMIL_PHRASE, "muse-spark-1.3", 1);
        when(repository.findBySlot(eq(ProctorPurpose.GREETING), eq(ProctorActivity.NONE), eq(1), eq("en"),
                eq(ProctorStatus.APPROVED))).thenReturn(List.of(approved));
        ProctorTextService.ResolvedText resolved = service.resolve(greeting, "Asha");
        assertTrue(resolved.fromMeta());
        assertTrue(resolved.text().startsWith("Hello, Asha!"));
        verify(gateway, never()).completeJson(any());
    }

    @Test
    void anEmptyNameBecomesThere() {
        assertTrue(service.resolve(greeting, "  ").text().contains("there"));
    }

    @Test
    void generateCandidateStoresAValidTextAsPending() {
        when(gateway.completeJson(any())).thenReturn(AiResult.success(GOOD_JSON, 200, 1200, 150, 60));
        assertEquals(ProctorTextService.GenerationOutcome.CREATED, service.generateCandidate(greeting, false));
        verify(repository).save(any(ProctorText.class));
    }

    @Test
    void generateCandidateRejectsATextThatFailsTheChecks() {
        String bad = "{\"english\":\"You will get a high score and pass the test with great marks today, friend.\","
                + "\"phrase\":\"" + TAMIL_PHRASE + "\"}";
        when(gateway.completeJson(any())).thenReturn(AiResult.success(bad, 200, 900, 100, 40));
        assertEquals(ProctorTextService.GenerationOutcome.REJECTED, service.generateCandidate(greeting, false));
        verify(repository, never()).save(any(ProctorText.class));
    }

    @Test
    void generateCandidateReportsAGatewayFailureWithoutStoringAnything() {
        when(gateway.completeJson(any())).thenReturn(AiResult.failure(503, 800, "HTTP 503"));
        assertEquals(ProctorTextService.GenerationOutcome.FAILED, service.generateCandidate(greeting, false));
        verify(repository, never()).save(any(ProctorText.class));
    }

    @Test
    void generateCandidateSkipsWhenACandidateIsAlreadyWaiting() {
        ProctorText pending = new ProctorText(greeting, "x\ny", "muse-spark-1.3", 1);
        when(repository.findBySlot(eq(ProctorPurpose.GREETING), eq(ProctorActivity.NONE), eq(1), eq("en"),
                eq(ProctorStatus.PENDING))).thenReturn(List.of(pending));
        assertEquals(ProctorTextService.GenerationOutcome.SKIPPED, service.generateCandidate(greeting, false));
        verify(gateway, never()).completeJson(any());
    }
}
