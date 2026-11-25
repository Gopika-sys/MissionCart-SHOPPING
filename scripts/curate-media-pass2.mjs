import fs from 'node:fs';
import path from 'node:path';

const exact = [
  { key: 'elec_trimmer', term: 'Electric hair clipper (1).jpg' },
  { key: 'elec_mouse', term: 'Logitech G7 Wireless Cordless Mouse (2).jpg' },
  { key: 'elec_powerbank', term: 'Anker power bank and cable.jpg' },
  { key: 'study_notebook', term: 'Moleskine ruled notebook, inside view.jpg' },
  { key: 'study_stand', term: 'Laptop cooler.JPG' },
  { key: 'fash_watch', term: 'Battuta Watches.jpg' },
  { key: 'fash_tote', term: 'Reusable Bag 3.jpg' },
  { key: 'fash_sunglasses', term: 'Ray Ban Original Wayfarer.jpg' },
  { key: 'fit_mat', term: 'Oakley yoga mat.jpg' },
  { key: 'fit_dumbbell', term: 'Hantel.jpg' },
  { key: 'trav_luggage', term: 'Hard-shell spinner luggage' }
];

const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const outDir = path.join(process.cwd(), 'frontend', 'public', 'verified-media');
const manifestPath = path.join(process.cwd(), 'scripts', 'curated-media.json');
const manifest = fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) : {};
delete manifest.trav_suitcase;

for (const item of exact) {
  await sleep(750);
  const url = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=${encodeURIComponent(item.term)}&gsrlimit=4&prop=imageinfo&iiprop=url|size|mime|extmetadata&iiurlwidth=800&format=json&origin=*`;
  try {
    const res = await fetch(url, { headers: { 'User-Agent': 'MissionCartCatalogVerifier/1.0' } });
    if (!res.ok) continue;
    const data = await res.json();
    const pages = Object.values(data?.query?.pages || {});
    for (const p of pages) {
      const info = p.imageinfo?.[0];
      if (!info || !['image/jpeg', 'image/png', 'image/webp'].includes(info.mime)) continue;
      const remoteUrl = info.thumburl || info.url;
      const imgRes = await fetch(remoteUrl, { headers: { 'User-Agent': 'MissionCartCatalogVerifier/1.0' } });
      if (!imgRes.ok) continue;
      const buf = Buffer.from(await imgRes.arrayBuffer());
      if (buf.length < 4000) continue;
      const ext = info.mime === 'image/png' ? 'png' : 'jpg';
      const localFile = `${item.key}.${ext}`;
      fs.writeFileSync(path.join(outDir, localFile), buf);
      const meta = info.extmetadata || {};
      const license = (meta.LicenseShortName?.value || 'CC BY-SA').replace(/<[^>]+>/g, '').trim();
      const attribution = (meta.Artist?.value || 'Wikimedia Commons').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim().slice(0, 150);
      manifest[item.key] = { localPath: `/verified-media/${localFile}`, remoteSourceUrl: remoteUrl, title: p.title, license, attribution, source: 'Wikimedia Commons' };
      console.log(`SAVED ${item.key} -> ${localFile}`);
      break;
    }
  } catch (e) { console.log(`ERR ${item.key}: ${e.message}`); }
}
fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
console.log('Total curated assets:', Object.keys(manifest).length);
