# Speech service (Meta MMS-TTS, CPU). Build context: the tts/ folder.
FROM python:3.13-slim

ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1 HF_HOME=/models

WORKDIR /app
COPY requirements.txt .
RUN pip install --index-url https://download.pytorch.org/whl/cpu "torch==2.15.0" \
 && pip install -r requirements.txt

# The model weights go into the image, so the cluster never needs to reach Hugging Face.
RUN python -c "from transformers import AutoTokenizer, VitsModel; \
[ (AutoTokenizer.from_pretrained(r), VitsModel.from_pretrained(r)) for r in ('facebook/mms-tts-tam', 'facebook/mms-tts-hin', 'facebook/mms-tts-eng') ]"
ENV HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1

COPY server.py textnorm.py voicefx.py ./
RUN useradd --system --uid 10001 tts && chown -R tts /app
USER 10001
EXPOSE 8080
CMD ["python", "server.py"]
