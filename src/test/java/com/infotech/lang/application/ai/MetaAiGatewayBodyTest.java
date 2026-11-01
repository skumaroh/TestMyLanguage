package com.infotech.lang.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetaAiGatewayBodyTest {

    private final MetaAiGateway gateway = new MetaAiGateway(null, "https://example.invalid/v1", "key", "test-model", 5);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Object> schema = Map.of("type", "object");

    @Test
    void aTextRequestKeepsThePromptAsPlainContent() throws Exception {
        JsonNode body = mapper.readTree(gateway.buildBody(new AiRequest("T", "sys", "task", schema)));
        assertEquals("task", body.path("messages").path(1).path("content").asText());
    }

    @Test
    void aRequestWithAPictureSendsTextAndAnInlineImage() throws Exception {
        JsonNode body = mapper.readTree(gateway.buildBody(new AiRequest("T", "sys", "task", schema, new byte[] {1, 2, 3})));
        JsonNode parts = body.path("messages").path(1).path("content");
        assertTrue(parts.isArray());
        assertEquals("text", parts.path(0).path("type").asText());
        assertEquals("task", parts.path(0).path("text").asText());
        assertEquals("image_url", parts.path(1).path("type").asText());
        assertEquals("data:image/png;base64,AQID", parts.path(1).path("image_url").path("url").asText());
    }
}
