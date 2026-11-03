// Turns English letters into Tamil or Hindi letters as the learner types ("naan" -> நான்). It works word by word on the
// letters typed since the last space, only when the learner types at the end of the box; pasted text, text typed with the
// device's own Tamil/Hindi keyboard and edits in the middle are left untouched. The rules are simple and phonetic:
// they will not always match textbook spelling, so a model judges the meaning, not the spelling.

const TAMIL = {
  consonants: {
    // clusters whose spelling depends on the neighbouring letter (each ends in a consonant, like the others)
    ndr: 'ன்ற', nth: 'ந்த', rch: 'ற்ச', rk: 'ற்க', rp: 'ற்ப', tr: 'ற்ற',
    ng: 'ங', nj: 'ஞ', ny: 'ஞ', zh: 'ழ', sh: 'ஷ', ch: 'ச', th: 'த', dh: 'த',
    k: 'க', g: 'க', s: 'ச', t: 'ட', d: 'ட', N: 'ண', n: 'ன', p: 'ப', b: 'ப', m: 'ம', y: 'ய', r: 'ர', R: 'ற',
    l: 'ல', L: 'ள', v: 'வ', w: 'வ', j: 'ஜ', h: 'ஹ', f: 'ஃப', c: 'ச', q: 'க', x: 'க்ஸ', z: 'ஸ',
  },
  vowels: {
    aa: ['ஆ', 'ா'], ee: ['ஈ', 'ீ'], ii: ['ஈ', 'ீ'], oo: ['ஊ', 'ூ'], uu: ['ஊ', 'ூ'], ai: ['ஐ', 'ை'], au: ['ஔ', 'ௌ'],
    ae: ['ஏ', 'ே'], ou: ['ஔ', 'ௌ'],
    a: ['அ', ''], A: ['ஆ', 'ா'], i: ['இ', 'ி'], u: ['உ', 'ு'], e: ['எ', 'ெ'], E: ['ஏ', 'ே'], o: ['ஒ', 'ொ'], O: ['ஓ', 'ோ'],
  },
  halant: '்',
  halantAtEnd: true,
  nAtStart: 'ந',
};

const HINDI = {
  consonants: {
    chh: 'छ', kh: 'ख', gh: 'घ', ch: 'च', jh: 'झ', Th: 'ठ', Dh: 'ढ', th: 'थ', dh: 'ध', ph: 'फ', bh: 'भ', sh: 'श', Sh: 'ष',
    k: 'क', g: 'ग', c: 'च', j: 'ज', T: 'ट', D: 'ड', N: 'ण', t: 'त', d: 'द', n: 'न', p: 'प', f: 'फ़', b: 'ब', m: 'म',
    y: 'य', r: 'र', l: 'ल', v: 'व', w: 'व', s: 'स', h: 'ह', q: 'क', x: 'क्स', z: 'ज़',
  },
  vowels: {
    aa: ['आ', 'ा'], ee: ['ई', 'ी'], ii: ['ई', 'ी'], oo: ['ऊ', 'ू'], uu: ['ऊ', 'ू'], ai: ['ऐ', 'ै'], au: ['औ', 'ौ'],
    a: ['अ', ''], A: ['आ', 'ा'], i: ['इ', 'ि'], u: ['उ', 'ु'], e: ['ए', 'े'], E: ['ए', 'े'], o: ['ओ', 'ो'], O: ['ओ', 'ो'],
  },
  halant: '्',
  halantAtEnd: false,
  nAtStart: null,
};

function longest(table, text, i) {
  for (let len = 3; len >= 1; len--) {
    const piece = text.substr(i, len);
    if (piece.length === len && Object.prototype.hasOwnProperty.call(table, piece)) return piece;
  }
  return null;
}

// One word of English letters -> one word of the language ('tamil' or 'hindi').
function convertWord(raw, language) {
  if (language !== 'hindi') {
    // the Tamil plural ending -கள் (typed "kal", "gal", "ngal") is spelled with ள், which letter rules alone cannot guess
    const plural = /^(.{2,}?)(n?)(k|g)al$/i.exec(raw);
    if (plural) {
      const stem = plural[1];
      return convertWord(stem, language) + (plural[2] ? 'ங்கள்' : 'கள்');
    }
  }
  const rules = language === 'hindi' ? HINDI : TAMIL;
  // a capital first letter is just sentence case, not the special capital letters (N, T, ...)
  const text = raw.charAt(0).toLowerCase() + raw.slice(1);
  let out = '';
  let pending = false; // the output ends with a consonant that has no vowel sign yet
  let i = 0;
  while (i < text.length) {
    const consonant = longest(rules.consonants, text, i);
    if (consonant) {
      if (pending) out += rules.halant;
      let letter = rules.consonants[consonant];
      if (consonant === 'n' && i === 0 && rules.nAtStart) letter = rules.nAtStart;
      out += letter;
      pending = true;
      i += consonant.length;
      continue;
    }
    const vowel = longest(rules.vowels, text, i);
    if (vowel) {
      out += pending ? rules.vowels[vowel][1] : rules.vowels[vowel][0];
      pending = false;
      i += vowel.length;
      continue;
    }
    if (pending && rules.halantAtEnd) out += rules.halant;
    pending = false;
    out += text.charAt(i);
    i += 1;
  }
  if (pending && rules.halantAtEnd) out += rules.halant;
  return out;
}

// Keeps, for every word typed through the converter, the English letters and where the word sits in the box, so a spelling
// suggestion can replace exactly that word (or a run of words) and nothing else. The list is published as JSON in the
// data-words attribute: [{raw, shown, start, end}]. Words whose text was edited by hand are left out.
function attach(host, language, tries = 0) {
  if (host.__tmlAttached) return;
  const textarea = host.querySelector('textarea') || host.inputElement;
  if (!textarea) {
    // the field may not be drawn yet; try again shortly
    if (tries < 40) setTimeout(() => attach(host, language, tries + 1), 50);
    return;
  }
  host.__tmlAttached = true;
  let words = []; // {raw, start, end, edited}
  let raw = ''; // English letters of the word being typed ('' when between words)
  let shown = textarea.value || '';
  const publish = () => {
    const list = words.filter((w) => !w.edited && w.raw !== '' && w.end > w.start)
      .map((w) => ({ raw: w.raw, shown: shown.slice(w.start, w.end), start: w.start, end: w.end }));
    host.setAttribute('data-words', JSON.stringify(list));
  };
  const setText = (text) => {
    shown = text;
    textarea.value = text;
    host.value = text;
    textarea.setSelectionRange(text.length, text.length);
  };
  const current = () => words[words.length - 1];

  // Called by the page after it put text into the box itself (for example words that were spoken): positions are no longer known.
  host.__tmlSync = () => {
    raw = '';
    shown = textarea.value || '';
    words.forEach((w) => { w.edited = true; });
    publish();
  };

  // Replaces the text between start and end by `text` when that span still holds `expected`. Returns true when done.
  host.__tmlReplace = (start, end, expected, text) => {
    if (shown.slice(start, end) !== expected) return false;
    const covered = words.filter((w) => w.start >= start && w.end <= end);
    const before = words.filter((w) => w.end <= start);
    const after = words.filter((w) => w.start >= end);
    const delta = text.length - (end - start);
    const parts = text.split(' ');
    const middle = [];
    if (covered.length === parts.length) {
      let at = start;
      covered.forEach((w, i) => {
        middle.push({ raw: w.raw, start: at, end: at + parts[i].length, edited: w.edited });
        at += parts[i].length + 1;
      });
    } else if (covered.length > 0) {
      middle.push({ raw: covered.map((w) => w.raw).join(' '), start, end: start + text.length, edited: true });
    }
    after.forEach((w) => { w.start += delta; w.end += delta; });
    words = before.concat(middle, after);
    raw = '';
    setText(shown.slice(0, start) + text + shown.slice(end));
    publish();
    return true;
  };

  textarea.addEventListener('input', (event) => {
    const value = textarea.value;
    const atEnd = textarea.selectionStart === value.length;
    const letter = event.inputType === 'insertText' && /^[A-Za-z]$/.test(event.data || '');
    if (letter && atEnd && value === shown + event.data) {
      if (raw === '') words.push({ raw: '', start: shown.length, end: shown.length, edited: false });
      raw += event.data;
      const w = current();
      w.raw = raw;
      setText(shown.slice(0, w.start) + convertWord(raw, language));
      w.end = shown.length;
    } else if (event.inputType === 'deleteContentBackward' && raw !== '' && atEnd && shown.startsWith(value)) {
      raw = raw.slice(0, -1);
      const w = current();
      if (raw === '') {
        setText(shown.slice(0, w.start));
        words.pop();
      } else {
        w.raw = raw;
        setText(shown.slice(0, w.start) + convertWord(raw, language));
        w.end = shown.length;
      }
    } else if (event.inputType === 'insertText' && event.data === ' ' && atEnd && value === shown + ' ') {
      raw = '';
      shown = value;
    } else if (event.inputType === 'deleteContentBackward' && raw === '' && atEnd && shown.length === value.length + 1
               && shown.startsWith(value)) {
      // backspace over a space or into a finished word: that word is now hand-edited
      shown = value;
      words.forEach((w) => { if (w.end > shown.length) { w.end = shown.length; w.edited = true; } });
    } else {
      // pasted text, the device's own keyboard, edits in the middle: positions can no longer be trusted
      raw = '';
      shown = value;
      words.forEach((w) => { w.edited = true; });
    }
    publish();
  });
}

window.tmlTransliterate = { attach, convertWord };
export { attach, convertWord };
