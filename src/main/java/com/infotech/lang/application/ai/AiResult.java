package com.infotech.lang.application.ai;

/**
 * Outcome of one model call. {@code content} is the model's JSON text when {@code ok} is true.
 * {@code error} is a short description with no prompt, answer or key in it.
 */
public record AiResult(boolean ok, String content, int httpStatus, long latencyMs,
                       Integer tokensIn, Integer tokensOut, String error) {

    public static AiResult success(String content, int httpStatus, long latencyMs, Integer tokensIn, Integer tokensOut) {
        return new AiResult(true, content, httpStatus, latencyMs, tokensIn, tokensOut, null);
    }

    public static AiResult failure(int httpStatus, long latencyMs, String error) {
        return new AiResult(false, null, httpStatus, latencyMs, null, null, error);
    }
}
