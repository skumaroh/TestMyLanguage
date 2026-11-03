package com.infotech.lang.application.ai;

/** Turns a short recording into text. The only way the application sends audio to a model, so it can be faked in tests. */
public interface SpeechToText {

    /**
     * @param wav      mono 16-bit PCM WAV, 16 or 24 kHz
     * @param language {@code tamil} or {@code hindi}
     * @return never null; failures come back in the result
     */
    Result transcribe(byte[] wav, String language);

    /** {@code text} is empty when nothing was recognised. {@code error} is a short description with no audio or key in it. */
    record Result(boolean ok, String text, long latencyMs, String error) {
        public static Result success(String text, long latencyMs) {
            return new Result(true, text == null ? "" : text.trim(), latencyMs, null);
        }

        public static Result failure(long latencyMs, String error) {
            return new Result(false, "", latencyMs, error);
        }
    }
}
