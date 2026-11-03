package com.infotech.lang.application.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoClipTest {

    @Test
    void aClipNeedsADescriptionAndAnAnswerBeforeItCountsAsComplete() {
        VideoClip c = new VideoClip("tamil", 1, "assessment-clips/a.mp4", "VIDEO", 10L);
        assertFalse(c.hasAnswer());
        c.setEnglish("The boy is swimming.");
        assertFalse(c.hasAnswer());
        c.setExpectedText("பையன் நீந்துகிறான்.");
        assertTrue(c.hasAnswer());
    }

    @Test
    void acceptedAnswersAreTheCanonicalOneThenTheVariants() {
        VideoClip c = new VideoClip("hindi", 1, "assessment-clips/b.mp4", "VIDEO", 10L);
        c.setExpectedText(" लड़का तैर रहा है। ");
        c.setVariants("लड़का तैरता है।; ; लड़का नहा रहा है।");
        assertEquals(List.of("लड़का तैर रहा है।", "लड़का तैरता है।", "लड़का नहा रहा है।"), c.acceptedAnswers());
    }

    @Test
    void editingAVerifiedClipGoesBackToDraftOnlyThroughTheLibrary() {
        VideoClip c = new VideoClip("tamil", 1, "assessment-clips/c.mp4", "VIDEO", 10L);
        c.markVerified(null);
        assertEquals(VideoClip.VERIFIED, c.getStatus());
        assertNull(c.getLastUsedAt());
        c.markUsed();
        assertEquals(1, c.getTimesUsed());
    }

    @Test
    void theFileKindComesFromTheExtensionAndOtherFilesAreIgnored() {
        assertEquals("VIDEO", VideoLibrary.kindOf("assessment-clips/Swim.MP4"));
        assertEquals("IMAGE", VideoLibrary.kindOf("assessment-clips/eat.png"));
        assertEquals("IMAGE", VideoLibrary.kindOf("assessment-clips/eat.JPG"));
        assertNull(VideoLibrary.kindOf("assessment-clips/notes.txt"));
        assertNull(VideoLibrary.kindOf("assessment-clips/"));
    }

    @Test
    void theMediaRouteServesVideoOnlyFromTheClipsContainer() {
        assertTrue(AssessmentMediaService.isAllowed("video", AssessmentMediaService.VIDEO_CONTAINER));
        assertFalse(AssessmentMediaService.isAllowed("video", AssessmentMediaService.AUDIO_CONTAINER));
        assertFalse(AssessmentMediaService.isAllowed("video", "some-other-container"));
        assertTrue(AssessmentMediaService.VIDEO_PREFIX.endsWith("/"));
    }
}
