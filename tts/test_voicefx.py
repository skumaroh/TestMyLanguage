"""Checks the pitch and speed processing on a synthetic tone. Run: python3 -I tts/test_voicefx.py (needs torch)."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import torch  # noqa: E402

import voicefx  # noqa: E402

SR = 16000


def peak_hz(x):
    spectrum = torch.abs(torch.fft.rfft(x * torch.hann_window(len(x))))
    return float(torch.argmax(spectrum)) * SR / len(x)


t = torch.arange(0, SR * 2) / SR
tone = 0.5 * torch.sin(2 * math.pi * 150 * t)
for semitones, rate in [(0, 1.0), (4, 1.0), (-3, 1.0), (0, 0.85), (5, 1.1)]:
    out = voicefx.apply(tone, semitones, rate)
    want_len = len(tone) / rate
    assert abs(len(out) - want_len) / want_len < 0.03, ("length", semitones, rate, len(out), want_len)
    want_hz = 150 * 2 ** (semitones / 12)
    assert abs(peak_hz(out) - want_hz) / want_hz < 0.03, ("pitch", semitones, rate, peak_hz(out), want_hz)
assert voicefx.apply(tone, float("nan"), float("inf")).shape == tone.shape  # bad numbers fall back to no change
assert voicefx.clamp(99, -8, 8, 0) == 8 and voicefx.clamp("x", 0, 1, 0.5) == 0.5
print("voicefx ok")
