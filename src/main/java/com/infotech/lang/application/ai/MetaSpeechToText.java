package com.infotech.lang.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Meta speech-to-text ({@code POST /asr/transcribe}, push-to-talk, WAV upload). The language name is always sent as the
 * language bias ("Tamil" / "Hindi"): without it the model may answer in English letters. The key comes from the environment
 * only and is never logged; audio is sent to Meta and is not stored by this application. Each call is recorded in
 * {@code assess_ai_call_log} (purpose, model, status, timing, a short error summary; never the audio or the text).
 */
@Component
public class MetaSpeechToText implements SpeechToText {

    private static final Logger logger = LoggerFactory.getLogger(MetaSpeechToText.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AiCallLogRepository callLog;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public MetaSpeechToText(AiCallLogRepository callLog,
                            @Value("${meta.api.base-url:https://api.meta.ai/v1}") String baseUrl,
                            @Value("${meta.api.key:}") String apiKey,
                            @Value("${meta.asr.model:muse-voice-transcribe-1.0}") String model,
                            @Value("${meta.asr.timeout-seconds:30}") int timeoutSeconds) {
        this.callLog = callLog;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Result transcribe(byte[] wav, String language) {
        if (apiKey.isEmpty()) {
            Result result = Result.failure(0, "model key not configured");
            record("NOT_CONFIGURED", result, 0);
            return result;
        }
        long started = System.nanoTime();
        int status = 0;
        Result result;
        try {
            String boundary = "tml" + UUID.randomUUID().toString().replace("-", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/asr/transcribe"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, requestJson(language), wav)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            status = response.statusCode();
            long ms = (System.nanoTime() - started) / 1_000_000;
            result = status / 100 != 2 ? Result.failure(ms, "HTTP " + status) : parse(response.body(), ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = Result.failure((System.nanoTime() - started) / 1_000_000, "interrupted");
        } catch (IOException | RuntimeException e) {
            // only the exception type goes into the summary: messages can echo request details
            result = Result.failure((System.nanoTime() - started) / 1_000_000, e.getClass().getSimpleName());
        }
        record(result.ok() ? "OK" : "ERROR", result, status);
        return result;
    }

    String requestJson(String language) {
        try {
            var node = mapper.createObjectNode();
            node.put("mode", "PUSH_TO_TALK");
            node.put("model", model);
            node.put("audioEncoding", "WAV");
            node.putArray("languageBias").add(languageName(language));
            return mapper.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String languageName(String language) {
        return "hindi".equals(language == null ? "" : language.toLowerCase(Locale.ROOT)) ? "Hindi" : "Tamil";
    }

    static byte[] multipart(String boundary, String requestJson, byte[] wav) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(wav.length + 512);
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"request\"\r\n"
                + "Content-Type: application/json\r\n\r\n" + requestJson + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"speech.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(wav);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    Result parse(String body, long latencyMs) {
        try {
            JsonNode root = mapper.readTree(body);
            if (!root.has("transcript")) {
                return Result.failure(latencyMs, "no transcript field");
            }
            return Result.success(root.path("transcript").asText(""), latencyMs);
        } catch (IOException e) {
            return Result.failure(latencyMs, "unusable answer");
        }
    }

    private void record(String status, Result result, int httpStatus) {
        try {
            callLog.save(new AiCallLog("SPEECH_TO_TEXT", model, status, httpStatus == 0 ? null : httpStatus,
                    (int) Math.min(result.latencyMs(), Integer.MAX_VALUE), null, null, result.error()));
        } catch (RuntimeException e) {
            logger.warn("Could not write the AI call log ({})", e.getClass().getSimpleName());
        }
    }
}
