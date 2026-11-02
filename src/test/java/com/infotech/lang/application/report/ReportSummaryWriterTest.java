package com.infotech.lang.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportSummaryWriterTest {

    private static ReportSnapshot snapshot() {
        return new ReportSnapshot(7, "tamil", 1, "s", "c", 20, 16, 80.0, true, true, "Well done", List.of(
                new ReportSnapshot.Block("WORDS", "Words", 1, 4, 4, true),
                new ReportSnapshot.Block("TRANSLATION", "Translation", 4, 8, 5, false)),
                List.of(new ReportSnapshot.Question("TRANSLATION", 1, "I am a student.", "நான் மாணவன்", true, "நான் ஒரு மாணவன்.",
                        List.of("Spoken", "A few spelling slips"))), null, null);
    }

    @Test
    void theModelIsGivenOnlyTheComputedFactsNeverTheStudentsWords() {
        String task = ReportSummaryWriter.task(snapshot());
        assertTrue(task.contains("Words: 4 of 4 correct, passed"));
        assertTrue(task.contains("Translation: 5 of 8 correct, not passed"));
        assertTrue(task.contains("Overall: 16 of 20 correct (80%)"));
        assertTrue(task.contains("Answers given by voice: 1; answers with spelling slips: 1"));
        assertFalse(task.contains("நான் மாணவன்"));
        assertFalse(task.contains("I am a student"));
    }

    @Test
    void aGoodAnswerIsKeptAndABadOneGivesNothing() {
        ReportSummaryWriter ok = new ReportSummaryWriter(r -> AiResult.success("{\"summary\":\"You did well.\\n\\nPractise translation.\"}", 200, 5, 1, 1), "m");
        assertEquals("You did well.\n\nPractise translation.", ok.write(snapshot()).orElseThrow());
        assertTrue(new ReportSummaryWriter(r -> AiResult.failure(500, 5, "down"), "m").write(snapshot()).isEmpty());
        assertTrue(new ReportSummaryWriter(r -> AiResult.success("not json", 200, 5, 1, 1), "m").write(snapshot()).isEmpty());
        assertTrue(new ReportSummaryWriter(r -> AiResult.success("{\"summary\":\"\"}", 200, 5, 1, 1), "m").write(snapshot()).isEmpty());
        assertTrue(new ReportSummaryWriter(r -> {
            throw new IllegalStateException("boom");
        }, "m").write(snapshot()).isEmpty());
    }

    @Test
    void anOverlongSummaryIsRejected() {
        String longText = "x".repeat(ReportSummaryWriter.MAX_CHARS + 1);
        assertTrue(new ReportSummaryWriter(r -> AiResult.success("{\"summary\":\"" + longText + "\"}", 200, 5, 1, 1), "m")
                .write(snapshot()).isEmpty());
    }
}
