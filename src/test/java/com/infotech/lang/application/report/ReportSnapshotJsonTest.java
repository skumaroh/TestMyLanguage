package com.infotech.lang.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportSnapshotJsonTest {

    @Test
    void aSnapshotSurvivesBeingStoredAsJsonAndReadBack() throws Exception {
        ReportSnapshot original = new ReportSnapshot(9, "hindi", 1, "2026-11-02T01:00Z", "2026-11-02T01:20Z", 8, 6, 75.0, null, false,
                "partial", List.of(new ReportSnapshot.Block("WORDS", "Words", 1, 8, 6, true)),
                List.of(new ReportSnapshot.Question("WORDS", 1, "Build the word", "घर", true, "घर", List.of("Spoken"))),
                "You did well.", "muse-spark-1.3");
        ObjectMapper mapper = new ObjectMapper();
        ReportSnapshot back = mapper.readValue(mapper.writeValueAsString(original), ReportSnapshot.class);
        assertEquals(original, back);
        assertNull(back.levelPassed());
        assertEquals("घर", back.questions().get(0).expected());
    }

    @Test
    void withSummaryOnlyChangesTheSummary() {
        ReportSnapshot r = new ReportSnapshot(1, "tamil", 1, "s", "c", 4, 3, 75.0, true, true, "m", List.of(), List.of(), null, null);
        ReportSnapshot w = r.withSummary("Hello", "model-x");
        assertEquals("Hello", w.summary());
        assertEquals("model-x", w.summaryModel());
        assertEquals(r.totalCorrect(), w.totalCorrect());
        assertEquals(r.levelMessage(), w.levelMessage());
    }
}
