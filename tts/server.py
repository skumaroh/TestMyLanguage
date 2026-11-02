"""Speech service for the assessment apps: text in Tamil or Hindi in, a WAV out, using Meta's open-source MMS-TTS models
(facebook/mms-tts-tam, facebook/mms-tts-hin; CC-BY-NC 4.0, so non-commercial use only).

POST /tts   {"language": "tamil" | "hindi", "text": "..."}  ->  audio/wav (16-bit mono PCM)
GET  /healthz                                                 ->  200 once the models are loaded

Only the languages named in TTS_LANGUAGES (default "tamil,hindi") are loaded, so each language's pod can keep to one model.
The weights are baked into the image at build time (HF_HUB_OFFLINE=1 at run time). The service has no state and no secrets; it is
reachable only inside the cluster. It never logs the text it is asked to speak.
"""
import io
import json
import os
import re
import threading
import wave
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import numpy as np
import torch
from transformers import AutoTokenizer, VitsModel

REPOS = {"tamil": "facebook/mms-tts-tam", "hindi": "facebook/mms-tts-hin"}
SCRIPT = {"tamil": re.compile(r"[஀-௿]"), "hindi": re.compile(r"[ऀ-ॿ]")}
MAX_CHARS = 400
MAX_BODY = 4096

MODELS = {}
READY = threading.Event()
LOCK = threading.Lock()  # one synthesis at a time: the models run on the CPU


def load_models():
    for language in [x.strip() for x in os.environ.get("TTS_LANGUAGES", "tamil,hindi").split(",") if x.strip()]:
        if language not in REPOS:
            raise SystemExit("unknown language in TTS_LANGUAGES: " + language)
        tokenizer = AutoTokenizer.from_pretrained(REPOS[language])
        model = VitsModel.from_pretrained(REPOS[language]).eval()
        MODELS[language] = (tokenizer, model)
    READY.set()


def clean(language, text):
    """Keeps the text the voice can read: the language's script, spaces and basic punctuation."""
    text = re.sub(r"\s+", " ", text or "").strip()
    text = re.sub(r"[^ऀ-ॿ஀-௿ .,!?;:'\-]", "", text)
    return text[:MAX_CHARS] if SCRIPT[language].search(text) else ""


def synthesize(language, text):
    tokenizer, model = MODELS[language]
    inputs = tokenizer(text, return_tensors="pt")
    with LOCK, torch.no_grad():
        waveform = model(**inputs).waveform
    samples = np.clip(waveform.squeeze().numpy(), -1.0, 1.0)
    pcm = (samples * 32767).astype("<i2").tobytes()
    out = io.BytesIO()
    with wave.open(out, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(model.config.sampling_rate)
        w.writeframes(pcm)
    return out.getvalue()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):  # no access log: it could hold text
        pass

    def _send(self, status, body=b"", content_type="text/plain"):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/healthz":
            self._send(200 if READY.is_set() else 503, b"ok" if READY.is_set() else b"loading")
        else:
            self._send(404)

    def do_POST(self):
        if self.path != "/tts":
            return self._send(404)
        if not READY.is_set():
            return self._send(503, b"loading")
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY:
                return self._send(413)
            request = json.loads(self.rfile.read(length))
            language = str(request.get("language", "")).lower()
            if language not in MODELS:
                return self._send(422, b"language not available")
            text = clean(language, str(request.get("text", "")))
            if not text:
                return self._send(422, b"nothing to read")
            self._send(200, synthesize(language, text), "audio/wav")
        except (ValueError, KeyError):
            self._send(400)
        except Exception:  # never echo details
            self._send(500)


if __name__ == "__main__":
    load_models()
    port = int(os.environ.get("PORT", "8080"))
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
