// A full audio conversation: the persona speaks, then the microphone turns on by itself; when the learner has spoken and then stays quiet
// for two seconds the answer is sent, the persona replies in audio, and so on. The microphone is OFF while the persona speaks, so the
// persona's voice is never picked up. Started by a click (browsers need that for sound and for the microphone). Where the browser cannot
// record, the button is hidden and the page keeps its typing box.
import { blobToWav, toBase64 } from './voice-recorder.js';

const SILENCE_MS = 2000;      // quiet this long after speech = the answer is complete
const NO_SPEECH_MS = 12000;   // nothing said at all for this long = nothing heard
const MAX_MS = 40000;         // longest single answer
const SPEECH_MIN_MS = 200;    // sound must last this long to count as speech
const TICK_MS = 50;

function attach(host, startButton, stopButton, status, personaName, firstUrl) {
  if (host.__talkAttached) return;
  host.__talkAttached = true;
  if (!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder && (window.AudioContext || window.webkitAudioContext))) {
    startButton.style.display = 'none';
    status.textContent = 'Full audio is not available in this browser. Please type your answers.';
    return;
  }
  let stream = null;
  let ctx = null;
  let analyser = null;
  let player = null;
  let recorder = null;
  let timer = null;
  let active = false;
  let lastLine = false;

  const show = (icon, text) => { status.textContent = icon + '  ' + text; };

  const cleanup = () => {
    active = false;
    clearInterval(timer);
    try { if (recorder && recorder.state === 'recording') recorder.stop(); } catch (e) { /* ignore */ }
    if (player) { player.pause(); }
    if (stream) stream.getTracks().forEach((t) => t.stop());
    try { if (ctx) ctx.close(); } catch (e) { /* ignore */ }
    stream = null; ctx = null; recorder = null;
  };

  const finishSession = (message) => {
    cleanup();
    stopButton.style.display = 'none';
    startButton.style.display = '';
    show('', message || '');
  };

  const rms = () => {
    const data = new Float32Array(analyser.fftSize);
    analyser.getFloatTimeDomainData(data);
    let sum = 0;
    for (let i = 0; i < data.length; i++) sum += data[i] * data[i];
    return Math.sqrt(sum / data.length);
  };

  const listen = () => {
    if (!active) return;
    show('🎙', 'Your turn: I am listening…');
    const chunks = [];
    recorder = new MediaRecorder(stream);
    recorder.ondataavailable = (e) => { if (e.data && e.data.size) chunks.push(e.data); };
    let spoke = false;
    let loudMs = 0;
    let quietMs = 0;
    let elapsed = 0;
    let floor = 0;
    let calibrated = 0;
    let finished = false;
    const done = () => {
      if (finished) return;
      finished = true;
      clearInterval(timer);
      recorder.onstop = async () => {
        if (!active) return;
        if (!spoke) { show('⏳', 'Thinking…'); host.$server.nothingHeard(); return; }
        show('⏳', 'Thinking…');
        try {
          const wav = await blobToWav(new Blob(chunks, { type: recorder.mimeType }));
          host.$server.audioReady(toBase64(wav));
        } catch (e) {
          host.$server.nothingHeard();
        }
      };
      if (recorder.state === 'recording') recorder.stop();
    };
    recorder.start();
    timer = setInterval(() => {
      elapsed += TICK_MS;
      const level = rms();
      if (calibrated < 300) { // the first 0.3 s sets the background noise level
        floor = Math.max(floor, level);
        calibrated += TICK_MS;
        return;
      }
      const threshold = Math.max(0.02, floor * 2.5);
      if (level > threshold) {
        loudMs += TICK_MS;
        quietMs = 0;
        if (loudMs >= SPEECH_MIN_MS) spoke = true;
      } else {
        quietMs += TICK_MS;
        if (!spoke) loudMs = 0;
      }
      if (spoke && quietMs >= SILENCE_MS) done();
      else if (!spoke && elapsed >= NO_SPEECH_MS) done();
      else if (elapsed >= MAX_MS) done();
    }, TICK_MS);
  };

  const say = (url, last) => {
    if (!active) return;
    lastLine = !!last;
    show('🔊', personaName + ' is speaking…');
    if (!url) { setTimeout(() => (lastLine ? finishSession('The conversation has ended.') : listen()), 300); return; }
    player.onended = () => setTimeout(() => (lastLine ? finishSession('The conversation has ended.') : listen()), 250);
    player.onerror = () => (lastLine ? finishSession('The conversation has ended.') : listen());
    player.src = url;
    const started = player.play();
    if (started && started.catch) started.catch(() => (lastLine ? finishSession('The conversation has ended.') : listen()));
  };

  host.__say = (url) => say(url, false);
  host.__sayLast = (url) => say(url, true);
  host.__retry = (message) => { if (active) { show('🎙', message); setTimeout(listen, 1200); } };
  host.__giveUp = (message) => finishSession(message);

  startButton.addEventListener('click', async () => {
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
    } catch (e) {
      show('', 'The microphone is not allowed. Please allow it in your browser, or type your answers.');
      return;
    }
    const Ctx = window.AudioContext || window.webkitAudioContext;
    ctx = new Ctx();
    if (ctx.state === 'suspended') ctx.resume();
    analyser = ctx.createAnalyser();
    analyser.fftSize = 1024;
    ctx.createMediaStreamSource(stream).connect(analyser);
    player = new Audio(); // one player for the whole conversation, unlocked by this click
    active = true;
    startButton.style.display = 'none';
    stopButton.style.display = '';
    try { if (window.speechSynthesis) window.speechSynthesis.cancel(); } catch (e) { /* ignore */ }
    document.querySelectorAll('audio').forEach((a) => a.pause());
    host.$server.started();
    say(firstUrl, false);
  });

  stopButton.addEventListener('click', () => {
    finishSession('You can type your answers instead.');
    host.$server.stopped();
  });
}

window.tmlTalk = { attach };
export { attach };
