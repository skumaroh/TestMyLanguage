package com.infotech.lang.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calls the Meta Model API (OpenAI-compatible chat completions, JSON-schema structured output).
 * The key comes from the environment only ({@code MODEL_API_KEY}) and is never logged. Each call is recorded in
 * {@code assess_ai_call_log} (purpose, model, status, timing, token counts, a short error summary).
 */
@Component
public class MetaAiGateway implements AiGateway {

    private static final Logger logger = LoggerFactory.getLogger(MetaAiGateway.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AiCallLogRepository callLog;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public MetaAiGateway(AiCallLogRepository callLog,
                         @Value("${meta.api.base-url:https://api.meta.ai/v1}") String baseUrl,
                         @Value("${meta.api.key:}") String apiKey,
                         @Value("${meta.text.model:muse-spark-1.3}") String model,
                         @Value("${meta.api.timeout-seconds:25}") int timeoutSeconds) {
        this.callLog = callLog;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public AiResult completeJson(AiRequest request) {
        if (apiKey.isEmpty()) {
            AiResult result = AiResult.failure(0, 0, "model key not configured");
            record(request, "NOT_CONFIGURED", result);
            return result;
        }
        AiResult result = callOnce(request);
        if (!result.ok() && isTransient(result.httpStatus())) {
            sleepQuietly(1000);
            result = callOnce(request);
        }
        record(request, result.ok() ? "OK" : "ERROR", result);
        return result;
    }

    private AiResult callOnce(AiRequest request) {
        long started = System.nanoTime();
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildBody(request)))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long ms = (System.nanoTime() - started) / 1_000_000;
            if (response.statusCode() / 100 != 2) {
                return AiResult.failure(response.statusCode(), ms, "HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return AiResult.failure(response.statusCode(), ms, "empty answer");
            }
            JsonNode usage = root.path("usage");
            Integer in = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer out = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            return AiResult.success(content, response.statusCode(), ms, in, out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AiResult.failure(0, (System.nanoTime() - started) / 1_000_000, "interrupted");
        } catch (IOException | RuntimeException e) {
            // Only the exception type goes into the summary: messages can echo request details.
            return AiResult.failure(0, (System.nanoTime() - started) / 1_000_000, e.getClass().getSimpleName());
        }
    }

    private String buildBody(AiRequest request) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemPrompt());
        messages.addObject().put("role", "user").put("content", request.userPrompt());
        ObjectNode format = body.putObject("response_format");
        format.put("type", "json_schema");
        ObjectNode schema = format.putObject("json_schema");
        schema.put("name", "Result");
        schema.set("schema", mapper.valueToTree(request.jsonSchema()));
        return mapper.writeValueAsString(body);
    }

    private static boolean isTransient(int httpStatus) {
        return httpStatus == 0 || httpStatus == 429 || httpStatus >= 500;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void record(AiRequest request, String status, AiResult result) {
        try {
            callLog.save(new AiCallLog(request.purpose(), model, status,
                    result.httpStatus() == 0 ? null : result.httpStatus(),
                    (int) Math.min(result.latencyMs(), Integer.MAX_VALUE),
                    result.tokensIn(), result.tokensOut(), result.error()));
        } catch (RuntimeException e) {
            // The call log is best effort: a missing table must not break the feature or the start-up.
            logger.warn("Could not write the AI call log ({})", e.getClass().getSimpleName());
        }
    }
}
