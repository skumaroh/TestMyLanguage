package com.infotech.lang.application.ai;

/** Turns a line of text into speech. The only way the application asks for audio, so it can be faked in tests. */
public interface TextToSpeech {

    /** True when a speech service is configured; when false callers use the browser's own voice. */
    boolean isConfigured();

    /**
     * @param language {@code tamil} or {@code hindi}
     * @return never null; failures come back in the result
     */
    Result synthesize(String language, String text);

    /** Same, with the voice coloured (pitch and speed). Services that cannot colour a voice just speak in the base voice. */
    default Result synthesize(String language, String text, VoiceStyle style) {
        return synthesize(language, text);
    }

    /** {@code wav} is 16-bit mono PCM. {@code error} is a short description with no text in it. */
    record Result(boolean ok, byte[] wav, long latencyMs, String error) {
        public static Result success(byte[] wav, long latencyMs) {
            return new Result(true, wav, latencyMs, null);
        }

        public static Result failure(long latencyMs, String error) {
            return new Result(false, new byte[0], latencyMs, error);
        }
    }
}
