package com.infotech.lang.application.views.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MediaControllerTest {

    @Test
    void noOrUnknownRangeHeaderMeansWholeFile() {
        assertNull(MediaController.parseRange(null, 100));
        assertNull(MediaController.parseRange("items=0-5", 100));
        assertNull(MediaController.parseRange("bytes=abc-", 100));
    }

    @Test
    void parsesClosedOpenAndSuffixRanges() {
        assertArrayEquals(new long[] {0, 9}, MediaController.parseRange("bytes=0-9", 100));
        assertArrayEquals(new long[] {10, 99}, MediaController.parseRange("bytes=10-", 100));
        assertArrayEquals(new long[] {90, 99}, MediaController.parseRange("bytes=-10", 100));
        assertArrayEquals(new long[] {0, 99}, MediaController.parseRange("bytes=0-500", 100));
    }

    @Test
    void anOutOfBoundsRangeCannotBeSatisfied() {
        assertArrayEquals(new long[] {-1, -1}, MediaController.parseRange("bytes=200-300", 100));
        assertArrayEquals(new long[] {-1, -1}, MediaController.parseRange("bytes=50-10", 100));
    }
}
