package com.infotech.lang.application.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssessmentMediaServiceTest {

    @Test
    void audioKeyIsTheLowerCaseCodePointsJoinedWithDashes() {
        assertEquals("u0b95", AssessmentMediaService.audioKey("க"));
        assertEquals("u0915-u094d-u0937", AssessmentMediaService.audioKey("क्ष"));
    }

    @Test
    void indexesLetterRecordingsAndIgnoresWordRecordings() {
        Map<String, String> index = AssessmentMediaService.indexLetterAudio(List.of(
                "audio/tamil/20260507120000_u0B95.mp3",
                "audio/tamil/20260604_dog.mp3",
                "audio/tamil/20260507120000_u0B95-u0BCD.mp3"));
        assertEquals("audio/tamil/20260507120000_u0B95.mp3", index.get("u0b95"));
        assertEquals("audio/tamil/20260507120000_u0B95-u0BCD.mp3", index.get("u0b95-u0bcd"));
        assertEquals(2, index.size());
    }

    @Test
    void theNewestRecordingOfALetterWins() {
        Map<String, String> index = AssessmentMediaService.indexLetterAudio(List.of(
                "audio/tamil/20260101000000_u0B95.mp3",
                "audio/tamil/20260901000000_u0B95.mp3"));
        assertEquals("audio/tamil/20260901000000_u0B95.mp3", index.get("u0b95"));
    }

    @Test
    void acceptsAFileNamedOnlyWithTheKey() {
        assertTrue(AssessmentMediaService.indexLetterAudio(List.of("audio/hindi/u0915.mp3")).containsKey("u0915"));
    }
}
