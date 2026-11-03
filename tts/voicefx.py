"""Changes how a synthesized voice sounds: pitch (in semitones) and speaking speed. Plain PyTorch, no extra libraries.

Pitch is shifted by resampling (which moves the pitch and the voice's colour together, so a raised voice also sounds lighter, as a
higher voice does) and then time-stretching with a phase vocoder to give the original length back. Speed is a plain time-stretch.
Small shifts (a few semitones) sound natural; large ones sound robotic, so the server clamps the values.
"""
import math

import torch

N_FFT = 1024
HOP = 256
MAX_PITCH = 8.0
MIN_RATE = 0.7
MAX_RATE = 1.3


def clamp(value, low, high, default):
    try:
        v = float(value)
    except (TypeError, ValueError):
        return default
    if math.isnan(v) or math.isinf(v):
        return default
    return max(low, min(high, v))


def time_stretch(wave, rate):
    """Plays the wave `rate` times faster (rate above 1 shortens it) without changing its pitch. wave: 1-D float tensor."""
    if abs(rate - 1.0) < 1e-3 or wave.numel() < N_FFT * 2:
        return wave
    window = torch.hann_window(N_FFT)
    spec = torch.stft(wave, N_FFT, HOP, window=window, return_complex=True)  # (freq, frames)
    frames = spec.shape[1]
    steps = torch.arange(0, frames - 1, rate)
    phase_advance = torch.linspace(0, math.pi * HOP, spec.shape[0]).unsqueeze(1)
    phase = torch.angle(spec[:, 0])
    out = []
    padded = torch.cat([spec, torch.zeros(spec.shape[0], 2, dtype=spec.dtype)], dim=1)
    for step in steps:
        i = int(step)
        frac = float(step) - i
        a, b = padded[:, i], padded[:, i + 1]
        magnitude = (1 - frac) * torch.abs(a) + frac * torch.abs(b)
        out.append(torch.polar(magnitude, phase))
        delta = torch.angle(b) - torch.angle(a) - phase_advance.squeeze(1)
        delta = delta - 2 * math.pi * torch.round(delta / (2 * math.pi))
        phase = phase + phase_advance.squeeze(1) + delta
    stretched = torch.stack(out, dim=1)
    return torch.istft(stretched, N_FFT, HOP, window=window)


def shift_pitch(wave, semitones):
    """Raises (positive) or lowers (negative) the pitch by `semitones`, keeping the length."""
    if abs(semitones) < 0.05 or wave.numel() < N_FFT * 2:
        return wave
    factor = 2.0 ** (semitones / 12.0)
    new_length = max(1, int(wave.numel() / factor))
    resampled = torch.nn.functional.interpolate(wave.view(1, 1, -1), size=new_length, mode="linear", align_corners=False).view(-1)
    # the resampled wave is shorter by `factor`; stretching it back by 1/factor restores the length with the new pitch
    return time_stretch(resampled, 1.0 / factor)


def apply(wave, pitch=0.0, rate=1.0):
    """Applies a pitch shift (semitones) and a speed change (rate; 1 = unchanged) to a 1-D float tensor in [-1, 1]."""
    pitch = clamp(pitch, -MAX_PITCH, MAX_PITCH, 0.0)
    rate = clamp(rate, MIN_RATE, MAX_RATE, 1.0)
    out = shift_pitch(wave, pitch)
    out = time_stretch(out, rate)
    return torch.clamp(out, -1.0, 1.0)
