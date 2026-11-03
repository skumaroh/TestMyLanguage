// A small pad for drawing one character with a finger, pen or mouse. Exports a white-background PNG
// (longest side 512 px) in a 'drawing-exported' event, together with whether it is blank and the stroke count.
class DrawingPad extends HTMLElement {
  constructor() {
    super();
    this.strokes = 0;
    this.drawing = false;
    this.attachShadow({ mode: 'open' });
    this.shadowRoot.innerHTML = `
      <style>
        :host { display: block; max-width: 360px; width: 100%; }
        canvas { width: 100%; aspect-ratio: 1 / 1; border: 2px solid #9aa0a6; border-radius: 8px;
                 background: #fff; touch-action: none; display: block; }
      </style>
      <canvas aria-label="Drawing pad"></canvas>`;
  }

  connectedCallback() {
    this.canvas = this.shadowRoot.querySelector('canvas');
    this.ctx = this.canvas.getContext('2d');
    this.canvas.addEventListener('pointerdown', (e) => this.start(e));
    this.canvas.addEventListener('pointermove', (e) => this.move(e));
    ['pointerup', 'pointerleave', 'pointercancel'].forEach((n) => this.canvas.addEventListener(n, () => this.end()));
    this.resize();
    this.resizeObserver = new ResizeObserver(() => this.resize());
    this.resizeObserver.observe(this.canvas);
  }

  disconnectedCallback() {
    if (this.resizeObserver) this.resizeObserver.disconnect();
  }

  resize() {
    const rect = this.canvas.getBoundingClientRect();
    const size = Math.max(Math.round(rect.width || 320), 1);
    if (this.canvas.width === size && this.canvas.height === size) return;
    this.canvas.width = size;
    this.canvas.height = size;
    this.strokes = 0;
    this.paintBackground();
  }

  paintBackground() {
    this.ctx.fillStyle = '#fff';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
  }

  point(e) {
    const rect = this.canvas.getBoundingClientRect();
    return { x: (e.clientX - rect.left) * (this.canvas.width / rect.width),
             y: (e.clientY - rect.top) * (this.canvas.height / rect.height) };
  }

  start(e) {
    e.preventDefault();
    this.canvas.setPointerCapture(e.pointerId);
    this.drawing = true;
    this.strokes += 1;
    const p = this.point(e);
    this.ctx.lineWidth = Math.max(3, this.canvas.width / 36);
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
    this.ctx.strokeStyle = '#000';
    this.ctx.beginPath();
    this.ctx.moveTo(p.x, p.y);
    this.ctx.lineTo(p.x + 0.1, p.y + 0.1);
    this.ctx.stroke();
  }

  move(e) {
    if (!this.drawing) return;
    e.preventDefault();
    const p = this.point(e);
    this.ctx.lineTo(p.x, p.y);
    this.ctx.stroke();
  }

  end() {
    this.drawing = false;
  }

  clearPad() {
    this.strokes = 0;
    this.paintBackground();
  }

  exportDrawing() {
    const max = 512;
    const scale = Math.min(1, max / this.canvas.width);
    const out = document.createElement('canvas');
    out.width = Math.max(1, Math.round(this.canvas.width * scale));
    out.height = Math.max(1, Math.round(this.canvas.height * scale));
    const octx = out.getContext('2d');
    octx.fillStyle = '#fff';
    octx.fillRect(0, 0, out.width, out.height);
    octx.drawImage(this.canvas, 0, 0, out.width, out.height);
    this.dispatchEvent(new CustomEvent('drawing-exported', {
      detail: { dataUrl: out.toDataURL('image/png'), blank: this.strokes === 0, strokeCount: this.strokes },
    }));
  }
}

customElements.define('drawing-pad', DrawingPad);
