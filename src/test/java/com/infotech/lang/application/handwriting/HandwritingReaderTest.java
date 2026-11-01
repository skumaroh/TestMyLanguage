package com.infotech.lang.application.handwriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiRequest;
import com.infotech.lang.application.ai.AiResult;
import org.junit.jupiter.api.Test;

class HandwritingReaderTest {

    private static HandwritingReader readerAnswering(AiResult result) {
        AiGateway gateway = request -> result;
        return new HandwritingReader(gateway);
    }

    @Test
    void readsTheCharacterAndConfidence() {
        HandwritingReader reader = readerAnswering(AiResult.success("{\"character\":\"க\",\"confidence\":0.93}", 200, 120, 10, 5));
        HandwritingReader.Reading reading = reader.read(new byte[] {1}, "tamil");
        assertTrue(reading.ok());
        assertEquals("க", reading.character());
        assertEquals(0.93, reading.confidence(), 0.0001);
    }

    @Test
    void clampsConfidenceIntoZeroToOne() {
        HandwritingReader reader = new HandwritingReader(request -> AiResult.failure(0, 0, "x"));
        assertEquals(1.0, reader.parse("{\"character\":\"a\",\"confidence\":7}", 1).confidence(), 0.0);
        assertEquals(0.0, reader.parse("{\"character\":\"a\",\"confidence\":-2}", 1).confidence(), 0.0);
    }

    @Test
    void anUnreadableAnswerIsAFailureNotACharacter() {
        HandwritingReader reader = new HandwritingReader(request -> AiResult.failure(0, 0, "x"));
        assertFalse(reader.parse("not json", 1).ok());
    }

    @Test
    void aFailedCallIsReportedWithoutThrowing() {
        HandwritingReader reader = readerAnswering(AiResult.failure(503, 50, "HTTP 503"));
        HandwritingReader.Reading reading = reader.read(new byte[] {1}, "hindi");
        assertFalse(reading.ok());
        assertEquals("HTTP 503", reading.error());
    }

    @Test
    void theRequestCarriesTheDrawingAndNamesTheScript() {
        AiRequest[] seen = new AiRequest[1];
        HandwritingReader reader = new HandwritingReader(request -> {
            seen[0] = request;
            return AiResult.success("{\"character\":\"अ\",\"confidence\":0.8}", 200, 1, 1, 1);
        });
        reader.read(new byte[] {9, 8}, "hindi");
        assertEquals("HANDWRITING_READ", seen[0].purpose());
        assertEquals(2, seen[0].imagePng().length);
        assertTrue(seen[0].userPrompt().contains("Devanagari"));
        assertFalse(seen[0].userPrompt().contains("meant to draw"));
    }
}
