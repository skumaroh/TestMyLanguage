// Reads a line aloud with the browser's own voice (not a Meta product: the Meta model only returns text). Speech must start inside the
// click itself, so the click is handled here in the browser. Where the device has no voice for the language, the button says so.
function attach(button, text, tag) {
  if (button.__speakAttached) return;
  button.__speakAttached = true;
  const synth = window.speechSynthesis;
  const idle = button.textContent;
  if (synth) synth.getVoices(); // starts loading the voice list
  const say = (message) => {
    button.textContent = message;
    setTimeout(() => { button.textContent = idle; }, 4000);
  };
  button.addEventListener('click', () => {
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
  });
}

window.tmlSpeak = { attach };
export { attach };
