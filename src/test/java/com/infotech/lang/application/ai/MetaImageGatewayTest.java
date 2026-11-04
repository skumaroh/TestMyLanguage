package com.infotech.lang.application.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class MetaImageGatewayTest {

    private final MetaImageGateway gateway = new MetaImageGateway(mock(AiCallLogRepository.class), "https://example.test/v1",
            "/images/generations", "key", "muse-image-1.0", "");

    @Test
    void readsABase64PictureFromTheAnswer() {
        byte[] picture = {1, 2, 3};
        MetaImageGateway.Result r = gateway.parse(200, "{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString(picture) + "\"}]}");
        assertTrue(r.ok());
        assertArrayEquals(picture, r.bytes());
    }

    @Test
    void reportsFailuresWithoutEchoingTheAnswer() {
        assertEquals("HTTP 404", gateway.parse(404, "secret details").error());
        assertFalse(gateway.parse(200, "{\"data\":[]}").ok());
        assertFalse(gateway.parse(200, "not json").ok());
        assertFalse(gateway.parse(200, "{\"data\":[{\"url\":\"http://plain.example/x.png\"}]}").ok());
    }
}
