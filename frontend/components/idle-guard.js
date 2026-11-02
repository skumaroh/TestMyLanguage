// Closes a waiting question when the learner has done nothing for a while: after `quietMs` without a key, tap or click anywhere on
// the page a countdown of `warnSeconds` appears; any activity cancels it; at zero the server is told (idleTimeout).
function attach(host, warnEl, quietMs, warnSeconds) {
  if (host.__idleAttached) return;
  host.__idleAttached = true;
  const events = ['keydown', 'pointerdown', 'input', 'click'];
  let running = false;
  let quietTimer = null;
  let countdown = null;
  let left = 0;

  const clear = () => {
    clearTimeout(quietTimer);
    clearInterval(countdown);
    quietTimer = null;
    countdown = null;
    warnEl.style.display = 'none';
  };
  const arm = () => {
    clear();
    if (!running) return;
    quietTimer = setTimeout(() => {
      left = warnSeconds;
      warnEl.style.display = 'block';
      warnEl.textContent = `Are you still there? This conversation ends in ${left} seconds. Tap or type to keep going.`;
      countdown = setInterval(() => {
        left -= 1;
        if (left <= 0) {
          running = false;
          clear();
          host.$server.idleTimeout();
        } else {
          warnEl.textContent = `Are you still there? This conversation ends in ${left} seconds. Tap or type to keep going.`;
        }
      }, 1000);
    }, quietMs);
  };
  const onActivity = () => {
    if (!host.isConnected) { running = false; clear(); events.forEach((e) => document.removeEventListener(e, onActivity, true)); return; }
    if (running) arm();
  };
  events.forEach((e) => document.addEventListener(e, onActivity, true));
  host.__idleStart = () => { running = true; arm(); };
  host.__idleStop = () => { running = false; clear(); };
}

window.tmlIdle = { attach };
export { attach };
