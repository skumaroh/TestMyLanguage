// Draws a QR code of the certificate's verification link into an element, as an image (so it is also in the PDF). Scanning it opens the
// public verify page with the code filled in.
import qrcode from 'qrcode-generator';

function draw(container, code) {
  const url = window.location.origin + '/verify?code=' + encodeURIComponent(code);
  const qr = qrcode(0, 'M');
  qr.addData(url);
  qr.make();
  const img = document.createElement('img');
  img.src = qr.createDataURL(4, 2);
  img.alt = 'QR code to verify this certificate';
  img.style.width = '96px';
  img.style.height = '96px';
  container.replaceChildren(img);
}

window.tmlCertificateQr = { draw };
export { draw };
