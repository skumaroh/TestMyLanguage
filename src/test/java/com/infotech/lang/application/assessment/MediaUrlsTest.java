package com.infotech.lang.application.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MediaUrlsTest {

    @Test
    void buildsTheProxyAddressFromASavedBlobUrl() {
        assertEquals("/api/media/audio/language-tool-audio/audio/tamil/20260604_dog.mp3",
                MediaUrls.proxyUrl("audio", "https://acct.blob.core.windows.net/language-tool-audio/audio/tamil/20260604_dog.mp3"));
    }

    @Test
    void encodesSpacesAndNonAsciiOncePerSegment() {
        assertEquals("/api/media/image/language-tool-images/tamil/my%20pic.jpg",
                MediaUrls.proxyUrl("image", "https://acct.blob.core.windows.net/language-tool-images/tamil/my%20pic.jpg"));
    }

    @Test
    void returnsNullForBlankOrMalformedUrls() {
        assertNull(MediaUrls.proxyUrl("audio", null));
        assertNull(MediaUrls.proxyUrl("audio", "  "));
        assertNull(MediaUrls.proxyUrl("audio", "https://acct.blob.core.windows.net/language-tool-audio"));
    }

    @Test
    void buildsTheProxyAddressFromContainerAndBlobPath() {
        assertEquals("/api/media/audio/language-tool-audio/audio/tamil/20260507_u0B95.mp3",
                MediaUrls.forBlob("audio", "language-tool-audio", "audio/tamil/20260507_u0B95.mp3"));
    }
}
