package com.infotech.lang.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Asks Meta's image model (Muse Image) for a picture. Used only from the admin page, one picture per click. The route, model and size
 * are settings because Meta's documentation for the image route is still being confirmed: {@code meta.image.path},
 * {@code meta.image.model}, {@code meta.image.size}. The key is the same one the text model uses and is never logged. Each call is
 * recorded in {@code assess_ai_call_log} with the purpose {@code word-image}.
 */
@Component
public class MetaImageGateway {

    private static final Logger logger = LoggerFactory.getLogger(MetaImageGateway.class);

    /** {@code bytes} is set when ok; {@code error} is a short reason otherwise (never a key or a prompt). */
    public record Result(boolean ok, byte[] bytes, int httpStatus, String error) {
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AiCallLogRepository callLog;
    private final String baseUrl;
    private final String path;
    private final String apiKey;
    private final String model;
    private final String size;

    public MetaImageGateway(AiCallLogRepository callLog,
                            @Value("${meta.api.base-url:https://api.meta.ai/v1}") String baseUrl,
                            @Value("${meta.image.path:/images/generations}") String path,
                            @Value("${meta.api.key:}") String apiKey,
                            @Value("${meta.image.model:muse-image}") String model,
                            @Value("${meta.image.size:1024x1024}") String size) {
        this.callLog = callLog;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.size = size == null ? "" : size.trim();
    }

    public String model() {
        return model;
    }

    public boolean configured() {
        return !apiKey.isEmpty();
    }

    public Result generate(String prompt) {
        if (apiKey.isEmpty()) {
            return done(new Result(false, null, 0, "model key not configured"), "NOT_CONFIGURED", 0);
        }
        long started = System.nanoTime();
        Result result;
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("n", 1);
            if (!size.isEmpty()) {
                body.put("size", size);
            }
            body.put("response_format", "b64_json");
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            result = parse(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = new Result(false, null, 0, "interrupted");
        } catch (Exception e) {
            result = new Result(false, null, 0, e.getClass().getSimpleName());
        }
        return done(result, result.ok() ? "OK" : "ERROR", (System.nanoTime() - started) / 1_000_000);
    }

    /** Reads the answer: a base64 picture, or a web address of one (only https is fetched). */
    Result parse(int status, String json) {
        if (status / 100 != 2) {
            return new Result(false, null, status, "HTTP " + status);
        }
        try {
            JsonNode first = mapper.readTree(json).path("data").path(0);
            String b64 = first.path("b64_json").asText("");
            if (!b64.isEmpty()) {
                return new Result(true, Base64.getDecoder().decode(b64), status, "");
            }
            String url = first.path("url").asText("");
            if (url.startsWith("https://")) {
                HttpResponse<byte[]> picture = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (picture.statusCode() / 100 == 2) {
                    return new Result(true, picture.body(), status, "");
                }
                return new Result(false, null, picture.statusCode(), "picture download HTTP " + picture.statusCode());
            }
            return new Result(false, null, status, "no picture in the answer");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, null, status, "interrupted");
        } catch (Exception e) {
            return new Result(false, null, status, "unreadable answer (" + e.getClass().getSimpleName() + ")");
        }
    }

    private Result done(Result result, String status, long latencyMs) {
        try {
            callLog.save(new AiCallLog("word-image", model, status, result.httpStatus() == 0 ? null : result.httpStatus(),
                    (int) Math.min(latencyMs, Integer.MAX_VALUE), null, null, result.ok() ? null : result.error()));
        } catch (RuntimeException e) {
            logger.warn("Could not write the AI call log ({})", e.getClass().getSimpleName());
        }
        return result;
    }
}
