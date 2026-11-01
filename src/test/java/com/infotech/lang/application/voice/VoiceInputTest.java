package com.infotech.lang.application.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class VoiceInputTest {

    /** A WAV header with the given format followed by some samples. */
    private static String wav(int channels, int rate, int bits, int dataBytes) {
        byte[] b = new byte[44 + dataBytes];
        b[0] = 'R'; b[1] = 'I'; b[2] = 'F'; b[3] = 'F';
        b[8] = 'W'; b[9] = 'A'; b[10] = 'V'; b[11] = 'E';
        b[22] = (byte) channels;
        b[24] = (byte) rate; b[25] = (byte) (rate >> 8); b[26] = (byte) (rate >> 16);
        b[34] = (byte) bits;
        return Base64.getEncoder().encodeToString(b);
    }

    @Test
    void acceptsAMono16BitWavAt16Or24Khz() {
        assertTrue(VoiceInput.decodeWav(wav(1, 16000, 16, 1000)).isPresent());
        assertTrue(VoiceInput.decodeWav(wav(1, 24000, 16, 1000)).isPresent());
    }

    @Test
    void refusesOtherFormatsAndBadInput() {
        assertTrue(VoiceInput.decodeWav(wav(2, 16000, 16, 1000)).isEmpty());
        assertTrue(VoiceInput.decodeWav(wav(1, 44100, 16, 1000)).isEmpty());
        assertTrue(VoiceInput.decodeWav(wav(1, 16000, 8, 1000)).isEmpty());
        assertTrue(VoiceInput.decodeWav(null).isEmpty());
        assertTrue(VoiceInput.decodeWav("").isEmpty());
        assertTrue(VoiceInput.decodeWav("not base64!!").isEmpty());
        assertTrue(VoiceInput.decodeWav(Base64.getEncoder().encodeToString("hello world, not a wav file at all......................".getBytes())).isEmpty());
    }

    @Test
    void refusesTooLargeRecordings() {
        assertTrue(VoiceInput.decodeWav(wav(1, 16000, 16, VoiceInput.MAX_WAV_BYTES)).isEmpty());
        assertEquals(1, VoiceInput.decodeWav(wav(1, 16000, 16, 100)).map(x -> 1).orElse(0));
    }
}
