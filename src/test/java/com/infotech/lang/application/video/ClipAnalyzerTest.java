package com.infotech.lang.application.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.ai.AiResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClipAnalyzerTest {

    private static ClipAnalyzer answering(String json) {
        return new ClipAnalyzer(request -> AiResult.success(json, 200, 10, 1, 1));
    }

    private static String json(String english, String expected, String variants, String clear) {
        return "{\"english\":\"" + english + "\",\"expected_text\":\"" + expected + "\",\"variants\":" + variants
                + ",\"clear_enough\":\"" + clear + "\",\"problems\":\"none\",\"difficulty\":\"EASY\"}";
    }

    @Test
    void aGoodTamilDraftIsKept() {
        Optional<ClipDraft> d = answering(json("The boy is swimming.", "பையன் நீந்துகிறான்.", "[\"சிறுவன் நீந்துகிறான்.\"]", "YES"))
                .analyze("tamil", new byte[] {1}, true);
        assertTrue(d.isPresent());
        assertEquals("The boy is swimming.", d.get().english());
        assertEquals("பையன் நீந்துகிறான்.", d.get().expectedText());
        assertEquals(1, d.get().variants().size());
        assertTrue(d.get().clear());
        assertEquals("EASY", d.get().difficulty());
    }

    @Test
    void anAnswerNotInTheScriptOrWithoutADescriptionIsRejected() {
        assertTrue(answering(json("The boy is swimming.", "The boy swims", "[]", "YES")).analyze("tamil", new byte[] {1}, true).isEmpty());
        assertTrue(answering(json("The boy is swimming.", "लड़का तैर रहा है।", "[]", "YES")).analyze("tamil", new byte[] {1}, true).isEmpty());
        assertTrue(answering(json("", "பையன் நீந்துகிறான்.", "[]", "YES")).analyze("tamil", new byte[] {1}, true).isEmpty());
        assertTrue(answering(json("The boy is swimming.", "लड़का तैर रहा है।", "[]", "YES")).analyze("hindi", new byte[] {1}, true).isPresent());
    }

    @Test
    void variantsInTheWrongScriptOrDuplicatesAreDropped() {
        ClipDraft d = answering(json("A girl reads.", "लड़की पढ़ रही है।", "[\"girl reads\",\"लड़की पढ़ रही है।\",\"लड़की पढ़ती है।\"]", "YES"))
                .analyze("hindi", new byte[] {1}, true).orElseThrow();
        assertEquals(java.util.List.of("लड़की पढ़ती है।"), d.variants());
    }

    @Test
    void notClearIsKeptAsAnHonestVerdictAndFailuresGiveNothing() {
        assertFalse(answering(json("Two people play.", "இருவர் விளையாடுகிறார்கள்.", "[]", "NO")).analyze("tamil", new byte[] {1}, true)
                .orElseThrow().clear());
        assertTrue(new ClipAnalyzer(r -> AiResult.failure(500, 5, "down")).analyze("tamil", new byte[] {1}, true).isEmpty());
        assertTrue(answering("not json").analyze("tamil", new byte[] {1}, true).isEmpty());
        assertTrue(new ClipAnalyzer(r -> {
            throw new IllegalStateException("boom");
        }).analyze("tamil", new byte[] {1}, true).isEmpty());
    }

    @Test
    void thePromptAsksForSimpleSentencesAndNamesTheLanguage() {
        String video = ClipAnalyzer.task("tamil", true);
        assertTrue(video.contains("This clip"));
        assertTrue(video.contains("Tamil"));
        assertTrue(video.contains("very simple present-tense sentence"));
        assertTrue(ClipAnalyzer.task("hindi", false).contains("This picture"));
        assertTrue(ClipAnalyzer.task("hindi", false).contains("Hindi"));
    }
}
