// Records a short clip from the microphone and sends it to the server as a small 16 kHz mono 16-bit WAV (base64), which is
// what the Meta speech-to-text endpoint takes. Recording starts inside the click (browsers require it) and stops on the second
// click or after 20 seconds. Where the browser cannot record, the button is hidden and the learner types instead.
const RATE = 16000;
const MAX_MS = 20000;

function encodeWav(samples, rate) {
  const buffer = new ArrayBuffer(44 + samples.length * 2);
  const view = new DataView(buffer);
  const text = (offset, s) => { for (let i = 0; i < s.length; i++) view.setUint8(offset + i, s.charCodeAt(i)); };
  text(0, 'RIFF');
  view.setUint32(4, 36 + samples.length * 2, true);
  text(8, 'WAVE');
  text(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // mono
  view.setUint32(24, rate, true);
  view.setUint32(28, rate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  text(36, 'data');
  view.setUint32(40, samples.length * 2, true);
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(44 + i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
  return new Uint8Array(buffer);
}

function toBase64(bytes) {
  let binary = '';
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

async function blobToWav(blob) {
  const Ctx = window.AudioContext || window.webkitAudioContext;
  const ctx = new Ctx();
  try {
    const decoded = await ctx.decodeAudioData(await blob.arrayBuffer());
    const offline = new OfflineAudioContext(1, Math.max(1, Math.ceil(decoded.duration * RATE)), RATE);
    const source = offline.createBufferSource();
    source.buffer = decoded;
    source.connect(offline.destination);
    source.start();
    const rendered = await offline.startRendering();
    return encodeWav(rendered.getChannelData(0), RATE);
  } finally {
    ctx.close();
  }
}

function attach(host, button, status, idleLabel) {
  if (host.__voiceAttached) return;
  host.__voiceAttached = true;
  if (!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder)) {
    button.style.display = 'none';
    status.textContent = 'Voice input is not available in this browser. Please type your answer.';
    return;
  }
  let recorder = null;
  let stream = null;
  let timer = null;
  let startedAt = 0;
  const setIdle = (message) => {
    button.disabled = false;
    button.textContent = idleLabel;
    status.textContent = message || '';
  };
  host.__voiceDone = (message) => setIdle(message);

  const stop = () => { if (recorder && recorder.state === 'recording') recorder.stop(); };

  button.addEventListener('click', async () => {
    if (recorder && recorder.state === 'recording') { stop(); return; }
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
      setIdle('The microphone is not allowed. Please allow it in your browser, or type your answer.');
      return;
    }
    const chunks = [];
    recorder = new MediaRecorder(stream);
    recorder.ondataavailable = (e) => { if (e.data && e.data.size) chunks.push(e.data); };
    recorder.onstop = async () => {
      clearTimeout(timer);
      stream.getTracks().forEach((t) => t.stop());
      if (Date.now() - startedAt < 600) { setIdle('That was too short. Tap the button, say the sentence, tap again.'); return; }
      button.disabled = true;
      button.textContent = '…';
      status.textContent = 'Turning your voice into text…';
      try {
        const wav = await blobToWav(new Blob(chunks, { type: recorder.mimeType }));
        host.$server.audioReady(toBase64(wav));
      } catch (e) {
        setIdle('The recording could not be used. Please try again, or type your answer.');
      }
    };
    startedAt = Date.now();
    recorder.start();
    button.textContent = '⏹ Stop';
    status.textContent = 'Listening… tap again when you finish.';
    timer = setTimeout(stop, MAX_MS);
  });
}

window.tmlVoice = { attach, encodeWav };
export { attach, encodeWav };
