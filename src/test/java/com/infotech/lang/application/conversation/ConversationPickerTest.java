package com.infotech.lang.application.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConversationPickerTest {

    private static Conversation conv(long id, OffsetDateTime lastUsed) {
        Conversation c = new Conversation("tamil", 1, "META", "Scenario " + id, "ராணி");
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "lastUsedAt", lastUsed);
        return c;
    }

    @Test
    void readsConversationIdsFromItemReferencesAndIgnoresOthers() {
        Set<Long> ids = ConversationPicker.idsHad(List.of("conv:12:1", "conv:12:2", "conv:7:3", "asset:99", "conv:x:1", "sentence:4", "conv"));
        assertEquals(Set.of(12L, 7L), ids);
    }

    @Test
    void picksOnlyAConversationTheLearnerHasNotHad() {
        List<Conversation> verified = List.of(conv(1, null), conv(2, null), conv(3, null));
        for (int i = 0; i < 30; i++) {
            long id = ConversationPicker.chooseUnused(verified, Set.of(1L, 3L)).orElseThrow().getId();
            assertEquals(2L, id);
        }
    }

    @Test
    void nothingIsPickedWhenEveryConversationHasBeenHad() {
        assertTrue(ConversationPicker.chooseUnused(List.of(conv(1, null), conv(2, null)), Set.of(1L, 2L)).isEmpty());
        assertTrue(ConversationPicker.chooseUnused(List.of(), Set.of()).isEmpty());
    }

    @Test
    void theLeastRecentlyUsedIsRepeatedAndNeverUsedCountsAsOldest() {
        OffsetDateTime now = OffsetDateTime.now();
        Conversation a = conv(1, now.minusDays(1));
        Conversation b = conv(2, now.minusDays(5));
        Conversation c = conv(3, now.minusDays(2));
        assertEquals(2L, ConversationPicker.chooseLeastRecentlyUsed(List.of(a, b, c)).orElseThrow().getId());
        Conversation never = conv(4, null);
        assertEquals(4L, ConversationPicker.chooseLeastRecentlyUsed(List.of(a, b, never)).orElseThrow().getId());
        Optional<Conversation> none = ConversationPicker.chooseLeastRecentlyUsed(List.of());
        assertTrue(none.isEmpty());
    }
}
