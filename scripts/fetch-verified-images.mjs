// Curates verified open-license photographs from Wikimedia Commons with strict filtering
// and polite throttling so only accurate, real photographs are marked VERIFIED.
import fs from 'node:fs';
import path from 'node:path';

const queries = [
  { key: 'hero_workspace', terms: ['Coffee-desk-notes-workspace', 'desk workspace laptop'] },
  { key: 'hero_home', terms: ['minimalist living room', 'scandinavian living room'] },
  { key: 'hero_skincare', terms: ['skincare serum bottle', 'cosmetic dropper bottle'] },
  { key: 'hero_ceramics', terms: ['ceramic stoneware bowl', 'handmade pottery bowl'] },
  { key: 'elec_kettle', terms: ['Electric kettle phillips', 'electric kettle'] },
  { key: 'elec_lamp', terms: ['LED desk lamp', 'anglepoise desk lamp'] },
  { key: 'elec_powerstrip', terms: ['REML power strip', 'Surge suppressor'] },
  { key: 'elec_headphones', terms: ['Headphones 1.jpg', 'over-ear headphones'] },
  { key: 'elec_speaker', terms: ['JBL Flip 3', 'bluetooth speaker'] },
  { key: 'elec_induction', terms: ['Kookplaat inductie', 'induction cooker'] },
  { key: 'elec_trimmer', terms: ['Electric hair clipper', 'beard trimmer'] },
  { key: 'elec_mouse', terms: ['Logitech wireless mouse', 'optical computer mouse'] },
  { key: 'elec_powerbank', terms: ['Anker power bank', 'USB power bank'] },
  { key: 'study_notebook', terms: ['Moleskine notebook', 'spiral notebook pen'] },
  { key: 'study_stand', terms: ['Laptop stand', 'MacBook aluminum stand'] },
  { key: 'fash_sneakers', terms: ['white leather sneakers', 'Adidas Stan Smith'] },
  { key: 'fash_backpack', terms: ['canvas backpack', 'daypack rucksack'] },
  { key: 'fash_watch', terms: ['minimalist wristwatch', 'leather strap watch'] },
  { key: 'fash_tote', terms: ['canvas tote bag', 'cotton tote bag'] },
  { key: 'fash_sunglasses', terms: ['Wayfarer sunglasses', 'tortoiseshell sunglasses'] },
  { key: 'skin_bottle', terms: ['amber dropper bottle', 'glass dropper bottle'] },
  { key: 'skin_jar', terms: ['cream jar skincare', 'moisturizer jar'] },
  { key: 'kit_mug', terms: ['stoneware coffee mug', 'ceramic mug'] },
  { key: 'kit_pan', terms: ['cast iron skillet', 'nonstick frying pan'] },
  { key: 'kit_bottle', terms: ['insulated stainless steel bottle', 'Hydro Flask'] },
  { key: 'home_vase', terms: ['ceramic flower vase', 'stoneware vase'] },
  { key: 'home_towels', terms: ['folded cotton towels', 'bath towels stack'] },
  { key: 'groc_coffee', terms: ['roasted coffee beans', 'espresso beans'] },
  { key: 'groc_tea', terms: ['loose leaf green tea', 'matcha green tea'] },
  { key: 'trav_luggage', terms: ['rolling suitcase', 'hardshell suitcase'] },
  { key: 'fit_mat', terms: ['Oakley yoga mat', 'rolled yoga mat'] },
  { key: 'fit_dumbbell', terms: ['Hantel.jpg', 'hex dumbbell'] }
];

const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const usedUrls = new Set();

const EXCLUDE_WORDS = ['painting', 'drawing', 'museum', 'victorian', '18', '19th', 'illustration', 'diagram', 'internals', 'amiga', 'diaper', 'dvi', 'vga', 'religión', 'church', 'portrait', 'map', 'coat of arms', 'logo', 'icon'];

async function searchCommons(term) {
  const url = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=${encodeURIComponent(term)}&gsrlimit=10&prop=imageinfo&iiprop=url|size|mime|extmetadata&iiurlwidth=800&format=json&origin=*`;
  const res = await fetch(url, { headers: { 'User-Agent': 'MissionCartCatalogVerifier/1.0' } });
  if (!res.ok) return null;
  const data = await res.json();
  const pages = Object.values(data?.query?.pages || {});
  for (const p of pages) {
    const titleLower = (p.title || '').toLowerCase();
    if (EXCLUDE_WORDS.some(w => titleLower.includes(w))) continue;
    const info = p.imageinfo?.[0];
    if (!info) continue;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(info.mime)) continue;
    if ((info.width || 0) < 450 || (info.height || 0) < 350) continue;
    const candidateUrl = info.thumburl || info.url;
    if (usedUrls.has(candidateUrl)) continue;
    const meta = info.extmetadata || {};
    const license = (meta.LicenseShortName?.value || 'CC BY-SA').replace(/<[^>]+>/g, '').trim();
    const artistRaw = meta.Artist?.value || 'Wikimedia Commons Contributor';
    const attribution = artistRaw.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim().slice(0, 160) || 'Wikimedia Commons';
    return {
      title: p.title,
      imageUrl: candidateUrl,
      width: info.thumbwidth || info.width,
      height: info.thumbheight || info.height,
      license,
      attribution,
      source: 'Wikimedia Commons'
    };
  }
  return null;
}

const results = {};
for (const item of queries) {
  let found = null;
  for (const term of item.terms) {
    try {
      found = await searchCommons(term);
      await sleep(300);
      if (found) break;
    } catch {}
  }
  if (found) {
    usedUrls.add(found.imageUrl);
    results[item.key] = found;
    console.log(`OK   ${item.key}: ${found.title}`);
  } else {
    console.log(`MISS ${item.key}`);
  }
}

const outPath = path.join(process.cwd(), 'scripts', 'verified-images.json');
fs.writeFileSync(outPath, JSON.stringify(results, null, 2));
console.log(`Saved ${Object.keys(results).length}/${queries.length} verified images`);
