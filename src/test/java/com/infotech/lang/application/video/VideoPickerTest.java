package com.infotech.lang.application.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoPickerTest {

    private static VideoClip clip(long id, OffsetDateTime lastUsed) {
        VideoClip c = new VideoClip("tamil", 1, "assessment-clips/c" + id + ".mp4", "VIDEO", 100L);
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "lastUsedAt", lastUsed);
        return c;
    }

    @Test
    void readsClipIdsFromReferencesAndIgnoresOthers() {
        assertEquals(Set.of(4L, 9L), VideoPicker.idsHad(List.of("clip:4", "clip:9", "clip:4", "sentence:3", "clip:x", "clip", "conv:1:2")));
    }

    @Test
    void unseenClipsComeFirstAndNoClipIsPickedTwice() {
        List<VideoClip> verified = List.of(clip(1, null), clip(2, null), clip(3, null), clip(4, null), clip(5, null));
        for (int i = 0; i < 30; i++) {
            List<VideoClip> picked = VideoPicker.choose(verified, Set.of(1L, 2L), 3);
            assertEquals(3, picked.size());
            assertTrue(picked.stream().noneMatch(c -> c.getId() == 1L || c.getId() == 2L));
            assertEquals(3, new HashSet<>(picked.stream().map(VideoClip::getId).toList()).size());
        }
    }

    @Test
    void whenTooFewAreUnseenTheOnesUsedLongestAgoFillTheRest() {
        OffsetDateTime now = OffsetDateTime.now();
        VideoClip old = clip(1, now.minusDays(9));
        VideoClip recent = clip(2, now.minusDays(1));
        VideoClip never = clip(3, null);
        List<VideoClip> picked = VideoPicker.choose(List.of(recent, never, old), Set.of(1L, 2L), 3);
        assertEquals(List.of(3L, 1L, 2L), picked.stream().map(VideoClip::getId).toList());
    }

    @Test
    void neverMoreThanAskedForOrAvailable() {
        assertEquals(2, VideoPicker.choose(List.of(clip(1, null), clip(2, null), clip(3, null)), Set.of(), 2).size());
        assertEquals(1, VideoPicker.choose(List.of(clip(1, null)), Set.of(), 8).size());
        assertTrue(VideoPicker.choose(List.of(), Set.of(), 8).isEmpty());
    }
}
