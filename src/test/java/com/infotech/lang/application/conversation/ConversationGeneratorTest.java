package com.infotech.lang.application.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConversationGeneratorTest {

    private static String objectives(int n, String line) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= n; i++) {
            sb.append(i > 1 ? "," : "").append("{\"objective_en\":\"Say hello\",\"vocabulary_hint\":\"vanakkam\",\"fallback_line\":\"")
                    .append(line).append("\"}");
        }
        return sb.append("]").toString();
    }

    private static String json(int objectiveCount, String line, String intro) {
        return "{\"scenario_title\":\"Buying vegetables\",\"persona_name\":\"ராணி\",\"persona_description\":\"A seller\","
                + "\"scene_note\":\"Imagine...\",\"intro_line\":\"" + intro + "\",\"intro_english\":\"Hello!\","
                + "\"closing_line\":\"நன்றி, மீண்டும் வாருங்கள்.\",\"closing_english\":\"Thanks, come again.\","
                + "\"difficulty\":\"EASY\",\"objectives\":" + objectives(objectiveCount, line) + "}";
    }

    private static ConversationGenerator answering(String json) {
        return new ConversationGenerator(request -> AiResult.success(json, 200, 10, 1, 1));
    }

    @Test
    void acceptsAWellFormedTamilConversationWithEightObjectives() {
        Optional<GeneratedConversation> g = answering(json(8, "உங்களுக்கு என்ன வேண்டும்?", "வணக்கம்! வாருங்கள்."))
                .generate("tamil", 1, List.of());
        assertTrue(g.isPresent());
        assertEquals(8, g.get().objectives().size());
        assertEquals("ராணி", g.get().personaName());
        assertEquals("EASY", g.get().difficulty());
    }

    @Test
    void rejectsTheWrongNumberOfObjectives() {
        assertTrue(answering(json(7, "உங்களுக்கு என்ன வேண்டும்?", "வணக்கம்!")).generate("tamil", 1, List.of()).isEmpty());
        assertTrue(answering(json(9, "உங்களுக்கு என்ன வேண்டும்?", "வணக்கம்!")).generate("tamil", 1, List.of()).isEmpty());
    }

    @Test
    void rejectsLinesThatAreNotInTheTestedLanguage() {
        assertTrue(answering(json(8, "What do you want?", "வணக்கம்!")).generate("tamil", 1, List.of()).isEmpty());
        assertTrue(answering(json(8, "உங்களுக்கு என்ன வேண்டும்?", "Hello, welcome!")).generate("tamil", 1, List.of()).isEmpty());
        // a Tamil conversation is not accepted for Hindi
        assertTrue(answering(json(8, "உங்களுக்கு என்ன வேண்டும்?", "வணக்கம்!")).generate("hindi", 1, List.of()).isEmpty());
    }

    @Test
    void failuresGiveNothing() {
        assertTrue(new ConversationGenerator(r -> AiResult.failure(500, 5, "down")).generate("tamil", 1, List.of()).isEmpty());
        assertTrue(answering("not json").generate("tamil", 1, List.of()).isEmpty());
        assertTrue(new ConversationGenerator(r -> {
            throw new IllegalStateException("boom");
        }).generate("tamil", 1, List.of()).isEmpty());
    }

    @Test
    void scriptCheckHandlesMixedText() {
        assertTrue(ConversationGenerator.inScript("मैं एक छात्र हूँ।", "hindi"));
        assertTrue(ConversationGenerator.inScript("வணக்கம் ராணி", "tamil"));
        assertFalse(ConversationGenerator.inScript("hello", "hindi"));
        assertFalse(ConversationGenerator.inScript("", "tamil"));
        assertFalse(ConversationGenerator.inScript("   ", "tamil"));
    }

    @Test
    void thePromptCarriesTheRulesTheSituationAndTheExistingTitlesAsData() {
        String task = ConversationGenerator.task("tamil", 1, "a tea shop owner", List.of("Buying vegetables"));
        assertTrue(task.contains("a tea shop owner"));
        assertTrue(task.contains("Never ask the learner for real personal details"));
        assertTrue(task.contains("exactly 8 objectives"));
        assertTrue(task.contains("<<<EXISTING\nBuying vegetables\nEXISTING>>>"));
        assertFalse(ConversationGenerator.task("hindi", 1, "x", List.of()).contains("EXISTING"));
    }
}
