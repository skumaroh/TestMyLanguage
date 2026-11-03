package com.infotech.lang.application.ai;

import java.util.List;

/**
 * How a voice is coloured after Meta's MMS model has spoken: a pitch shift in semitones and a speed (1 = unchanged). Both are clamped by
 * the speech service. The MMS voices are single-speaker, so this is how a persona can sound lighter, deeper, older or younger.
 *
 * @param pitch semitones, positive = higher
 * @param rate  speed, above 1 = faster
 */
public record VoiceStyle(double pitch, double rate) {

    public static final VoiceStyle BASE = new VoiceStyle(0.0, 1.0);

    /** A named style for the admin voice lab and for the persona traits. Starting values only: they are tuned by ear in the voice lab. */
    public record Preset(String name, String gender, String ageBand, VoiceStyle style) {
    }

    /** The base voices are male, so female styles are shifted up. */
    public static List<Preset> presets() {
        return List.of(
                new Preset("Base voice", "UNKNOWN", "ADULT", BASE),
                new Preset("Female, adult", "FEMALE", "ADULT", new VoiceStyle(4.0, 1.0)),
                new Preset("Female, young", "FEMALE", "YOUNG", new VoiceStyle(5.5, 1.05)),
                new Preset("Female, old (grandmother)", "FEMALE", "OLD", new VoiceStyle(3.0, 0.88)),
                new Preset("Female, child", "FEMALE", "CHILD", new VoiceStyle(8.0, 1.1)),
                new Preset("Male, adult", "MALE", "ADULT", BASE),
                new Preset("Male, young", "MALE", "YOUNG", new VoiceStyle(1.5, 1.05)),
                new Preset("Male, old (grandfather)", "MALE", "OLD", new VoiceStyle(-1.5, 0.88)),
                new Preset("Male, child", "MALE", "CHILD", new VoiceStyle(6.0, 1.1)));
    }

    public boolean isBase() {
        return Math.abs(pitch) < 0.05 && Math.abs(rate - 1.0) < 0.01;
    }
}
