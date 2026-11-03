// Turns the certificate on the page into a downloadable A4 landscape PDF, in the browser (so any script, such as a Tamil or Hindi name,
// is drawn exactly as the page shows it). html2canvas takes a picture of the certificate and jsPDF puts it on the page.
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

async function download(element, filename) {
  const canvas = await html2canvas(element, { scale: 2, backgroundColor: '#ffffff', logging: false });
  const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  const maxW = 277; // A4 landscape 297 x 210 mm, with a 10 mm margin
  const maxH = 190;
  const ratio = Math.min(maxW / canvas.width, maxH / canvas.height);
  const w = canvas.width * ratio;
  const h = canvas.height * ratio;
  pdf.addImage(canvas.toDataURL('image/png'), 'PNG', (297 - w) / 2, (210 - h) / 2, w, h);
  pdf.save(filename);
}

window.tmlCertificate = { download };
export { download };
