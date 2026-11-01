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

// Keeps the English letters of the current word, so the shown text can be recomputed after every key.
function attach(host, language, tries = 0) {
  if (host.__tmlAttached) return;
  const textarea = host.querySelector('textarea') || host.inputElement;
  if (!textarea) {
    // the field may not be drawn yet; try again shortly
    if (tries < 40) setTimeout(() => attach(host, language, tries + 1), 50);
    return;
  }
  host.__tmlAttached = true;
  let raw = '';
  let wordStart = 0;
  let sentence = ''; // everything typed through the converter, in English letters (empty once the text is edited otherwise)
  let shown = textarea.value || '';
  const publish = () => host.setAttribute('data-raw', sentence);
  const show = (text) => {
    shown = text;
    textarea.value = text;
    host.value = text;
    textarea.setSelectionRange(text.length, text.length);
  };
  // lets the page clear the typed record after it replaced the text (for example by taking a suggestion)
  host.__tmlReset = () => {
    raw = '';
    sentence = '';
    shown = textarea.value || '';
    publish();
  };
  textarea.addEventListener('input', (event) => {
    const value = textarea.value;
    const atEnd = textarea.selectionStart === value.length;
    if (event.inputType === 'insertText' && /^[A-Za-z]$/.test(event.data || '') && atEnd && value === shown + event.data) {
      if (raw === '') wordStart = shown.length;
      if (shown === '') sentence = '';
      raw += event.data;
      sentence += event.data;
      show(shown.slice(0, wordStart) + convertWord(raw, language));
      publish();
    } else if (event.inputType === 'deleteContentBackward' && raw !== '' && atEnd && shown.startsWith(value)) {
      raw = raw.slice(0, -1);
      sentence = sentence.slice(0, -1);
      show(shown.slice(0, wordStart) + convertWord(raw, language));
      publish();
    } else if (event.inputType === 'insertText' && event.data === ' ' && atEnd && value === shown + ' ' && sentence !== '') {
      raw = '';
      sentence += ' ';
      shown = value;
      publish();
    } else if (event.inputType === 'deleteContentBackward' && raw === '' && atEnd && shown.startsWith(value)
               && sentence.endsWith(' ') && shown.endsWith(' ')) {
      sentence = sentence.slice(0, -1);
      shown = value;
      publish();
    } else {
      raw = '';
      sentence = '';
      shown = value;
      publish();
    }
  });
}

window.tmlTransliterate = { attach, convertWord };
export { attach, convertWord };
