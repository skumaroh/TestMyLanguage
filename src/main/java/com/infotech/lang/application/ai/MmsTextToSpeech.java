package com.infotech.lang.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calls the speech service in the cluster ({@code tts/}: Meta's open-source MMS-TTS models, CC-BY-NC 4.0). The service address comes
 * from {@code TTS_URL}; without it the application falls back to the browser's voice. Nothing is stored here and the text is never
 * logged. Each call is recorded in {@code assess_ai_call_log} (purpose, status, timing; never the text or the audio).
 */
@Component
public class MmsTextToSpeech implements TextToSpeech {

    private static final Logger logger = LoggerFactory.getLogger(MmsTextToSpeech.class);
    static final int MAX_CHARS = 400;
    private static final String MODEL = "mms-tts";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final AiCallLogRepository callLog;
    private final String baseUrl;
    private final int timeoutSeconds;

    public MmsTextToSpeech(AiCallLogRepository callLog,
                           @Value("${tts.url:}") String baseUrl,
                           @Value("${tts.timeout-seconds:20}") int timeoutSeconds) {
        this.callLog = callLog;
        String url = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isEmpty();
    }

    @Override
    public Result synthesize(String language, String text) {
        String line = text == null ? "" : text.trim();
        if (!isConfigured() || line.isEmpty()) {
            return Result.failure(0, "not configured");
        }
        if (line.length() > MAX_CHARS) {
            line = line.substring(0, MAX_CHARS);
        }
        long started = System.nanoTime();
        int status = 0;
        Result result;
        try {
            String body = mapper.writeValueAsString(Map.of("language", language(language), "text", line));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/tts"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            status = response.statusCode();
            long ms = (System.nanoTime() - started) / 1_000_000;
            result = status == 200 && response.body().length > 44 ? Result.success(response.body(), ms)
                    : Result.failure(ms, "HTTP " + status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = Result.failure((System.nanoTime() - started) / 1_000_000, "interrupted");
        } catch (IOException | RuntimeException e) {
            result = Result.failure((System.nanoTime() - started) / 1_000_000, e.getClass().getSimpleName());
        }
        record(result, status);
        return result;
    }

    static String language(String language) {
        return "hindi".equals(language == null ? "" : language.toLowerCase(Locale.ROOT)) ? "hindi" : "tamil";
    }

    private void record(Result result, int httpStatus) {
        try {
            callLog.save(new AiCallLog("TEXT_TO_SPEECH", MODEL, result.ok() ? "OK" : "ERROR", httpStatus == 0 ? null : httpStatus,
                    (int) Math.min(result.latencyMs(), Integer.MAX_VALUE), null, null, result.error()));
        } catch (RuntimeException e) {
            logger.warn("Could not write the AI call log ({})", e.getClass().getSimpleName());
        }
    }
}
