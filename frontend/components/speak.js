// Plays a line aloud. When an audio address is given (Meta's MMS voice from /api/tts/...) that is played; if it cannot be played the
// browser's own voice is used instead, and where nothing can speak the language the button says so. Playback starts inside the click
// itself (browsers require it), so the click is handled here in the browser.
function attach(button, text, tag, audioUrl) {
  if (button.__speakAttached) return;
  button.__speakAttached = true;
  const synth = window.speechSynthesis;
  const idle = button.textContent;
  if (synth) synth.getVoices(); // starts loading the voice list
  let audio = null;
  if (audioUrl) {
    audio = new Audio();
    audio.preload = 'none';
    audio.src = audioUrl;
    audio.addEventListener('ended', () => { button.textContent = idle; });
  }
  const say = (message) => {
    button.textContent = message;
    setTimeout(() => { button.textContent = idle; }, 4000);
  };
  const browserVoice = () => {
    if (!synth) { say('No voice in this browser'); return; }
    const lang = tag.toLowerCase();
    const base = lang.split('-')[0];
    const voices = synth.getVoices();
    const voice = voices.find((v) => v.lang.toLowerCase().replace('_', '-') === lang)
      || voices.find((v) => v.lang.toLowerCase().replace('_', '-').startsWith(base));
    if (!voice) { say('No voice for this language on this device'); return; }
    synth.cancel();
    synth.resume();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = tag;
    utterance.voice = voice;
    utterance.onend = () => { button.textContent = idle; };
    utterance.onerror = () => { button.textContent = idle; };
    button.textContent = '⏹ Playing…';
    synth.speak(utterance);
  };
  button.addEventListener('click', () => {
    if (synth) synth.cancel();
    if (!audio) { browserVoice(); return; }
    // one line at a time: stop any other line that is playing
    document.querySelectorAll('audio').forEach((a) => a.pause());
    if (window.__tmlPlaying && window.__tmlPlaying !== audio) window.__tmlPlaying.pause();
    window.__tmlPlaying = audio;
    audio.currentTime = 0;
    button.textContent = '⏳ Loading…';
    const started = audio.play();
    audio.onplaying = () => { button.textContent = '⏹ Playing…'; };
    if (started && started.catch) {
      started.catch(() => { button.textContent = idle; browserVoice(); });
    }
    audio.onerror = () => { button.textContent = idle; browserVoice(); };
  });
}

window.tmlSpeak = { attach };
export { attach };
