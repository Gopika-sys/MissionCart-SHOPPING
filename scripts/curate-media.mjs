import fs from 'node:fs';
import path from 'node:path';

const exactOrSearch = [
  { key: 'hero_workspace', term: 'Coffee-desk-notes-workspace (24243718641)' },
  { key: 'hero_study', term: 'Young woman taking notes and studying at desk' },
  { key: 'hero_kitchen', term: 'Ordinary chickpeas in a ceramic bowl' },
  { key: 'hero_skincare', term: 'Facial mask.jpg' },
  { key: 'elec_kettle', term: 'Electric kettle phillips white' },
  { key: 'elec_lamp', term: 'HK CWB Park Lane basement shop IKEA lighting desk stand' },
  { key: 'elec_powerstrip', term: 'REML power strip' },
  { key: 'elec_headphones', term: 'Headphones 1.jpg' },
  { key: 'elec_speaker', term: 'JBL Flip 3 bluetooth speaker' },
  { key: 'elec_induction', term: 'Kookplaat inductie' },
  { key: 'elec_trimmer', term: 'Electric hair clipper (1)' },
  { key: 'elec_mouse', term: 'Logitech G7 Wireless Cordless Mouse' },
  { key: 'elec_powerbank', term: 'Anker power bank and cable' },
  { key: 'study_notebook', term: 'Moleskine ruled notebook, inside view' },
  { key: 'study_stand', term: 'Laptop cooler.JPG' },
  { key: 'fash_watch', term: 'Battuta Watches' },
  { key: 'fash_tote', term: 'Reusable Bag 3' },
  { key: 'fash_sunglasses', term: 'Ray Ban Original Wayfarer' },
  { key: 'fit_mat', term: 'Oakley yoga mat' },
  { key: 'fit_dumbbell', term: 'Hantel.jpg' },
  { key: 'kit_mug', term: 'Cup of coffee' },
  { key: 'kit_pan', term: 'Cast iron skillet' },
  { key: 'groc_coffee', term: 'Roasted coffee beans' },
  { key: 'groc_oats', term: 'Rolled oats' },
  { key: 'home_vase', term: 'Porcelain vase' },
  { key: 'trav_suitcase', term: 'Samsonite suitcase' },
  { key: 'skin_dropper', term: 'Dropper bottle' },
  { key: 'fash_sneakers', term: 'Adidas Stan Smith' }
];

const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const outDir = path.join(process.cwd(), 'frontend', 'public', 'verified-media');
fs.mkdirSync(outDir, { recursive: true });

async function fetchOne(term) {
  const url = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=${encodeURIComponent(term)}&gsrlimit=6&prop=imageinfo&iiprop=url|size|mime|extmetadata&iiurlwidth=800&format=json&origin=*`;
  const res = await fetch(url, { headers: { 'User-Agent': 'MissionCartCatalogVerifier/1.0' } });
  if (!res.ok) return null;
  const data = await res.json();
  const pages = Object.values(data?.query?.pages || {});
  for (const p of pages) {
    const info = p.imageinfo?.[0];
    if (!info) continue;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(info.mime)) continue;
    const candidateUrl = info.thumburl || info.url;
    const meta = info.extmetadata || {};
    const license = (meta.LicenseShortName?.value || 'CC BY-SA').replace(/<[^>]+>/g, '').trim();
    const artistRaw = meta.Artist?.value || 'Wikimedia Commons Contributor';
    const attribution = artistRaw.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim().slice(0, 150) || 'Wikimedia Commons';
    return { title: p.title, remoteUrl: candidateUrl, mime: info.mime, width: info.thumbwidth || info.width, height: info.thumbheight || info.height, license, attribution };
  }
  return null;
}

const manifest = {};
for (const item of exactOrSearch) {
  try {
    const info = await fetchOne(item.term);
    await sleep(350);
    if (!info) { console.log(`MISS ${item.key}`); continue; }
    const ext = info.mime === 'image/png' ? 'png' : info.mime === 'image/webp' ? 'webp' : 'jpg';
    const localFile = `${item.key}.${ext}`;
    const imgRes = await fetch(info.remoteUrl, { headers: { 'User-Agent': 'MissionCartCatalogVerifier/1.0' } });
    if (!imgRes.ok) { console.log(`DL_FAIL ${item.key}`); continue; }
    const buf = Buffer.from(await imgRes.arrayBuffer());
    if (buf.length < 4000) { console.log(`TINY ${item.key}`); continue; }
    fs.writeFileSync(path.join(outDir, localFile), buf);
    manifest[item.key] = {
      localPath: `/verified-media/${localFile}`,
      remoteSourceUrl: info.remoteUrl,
      title: info.title,
      license: info.license,
      attribution: info.attribution,
      source: 'Wikimedia Commons'
    };
    console.log(`SAVED ${item.key} -> ${localFile} (${Math.round(buf.length / 1024)}KB)`);
  } catch (e) {
    console.log(`ERR ${item.key}: ${e.message}`);
  }
}

fs.writeFileSync(path.join(process.cwd(), 'scripts', 'curated-media.json'), JSON.stringify(manifest, null, 2));
console.log(`Total verified local assets: ${Object.keys(manifest).length}`);
