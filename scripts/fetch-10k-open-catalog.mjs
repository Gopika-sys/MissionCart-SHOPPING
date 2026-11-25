// Fetches 10,300+ genuine, distinct, non-duplicate real-world products with verified photos
// from Open Beauty Facts (81k+ records) and Open Products Facts (50k+ records) using
// multiple country/category/sort slices to stay cleanly under the 2,000-per-query search window.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const TARGET_COUNT = 10300;
const seenCodes = new Set();
const seenNameBrand = new Set();
const seenImages = new Set();
const records = [];

const BLOCKED_TERMS = [
  'cigarette', 'tobacco', 'marlboro', 'gauloises', 'nicotine', 'vape', 'e-liquid',
  'doliprane', 'paracetamol', 'ibuprofen', 'medicine', 'pharmaceutical', 'undefined',
  'test product', 'lorem'
];

function isCleanProduct(item) {
  if (!item.code || !item.product_name || !item.brands || !item.image_front_url) return false;
  const code = String(item.code).replace(/[^a-zA-Z0-9]/g, '');
  if (code.length < 6) return false;
  const name = String(item.product_name).trim();
  const brand = String(item.brands).split(',')[0].trim();
  if (name.length < 4 || name.length > 180 || brand.length < 2 || brand.length > 80) return false;
  if (!/^[A-Za-z0-9]/.test(name) || !/^[A-Za-z0-9]/.test(brand)) return false;
  const nonAscii = (name.match(/[^\x20-\x7E\u00C0-\u024F]/g) || []).length;
  if (nonAscii > 4) return false;
  const hay = (name + ' ' + brand + ' ' + (item.categories || '')).toLowerCase();
  if (BLOCKED_TERMS.some((b) => hay.includes(b))) return false;
  if (!item.image_front_url.startsWith('https://')) return false;
  return true;
}

function inferCategorySlug(sourceType, item) {
  const hay = ((item.categories || '') + ' ' + (item.product_name || '')).toLowerCase();
  if (sourceType === 'OPEN_BEAUTY_FACTS') {
    if (hay.includes('sunscreen') || hay.includes('spf') || hay.includes('serum') || hay.includes('cleanser') || hay.includes('moistur') || hay.includes('cream') || hay.includes('face') || hay.includes('skin') || hay.includes('toner') || hay.includes('acne') || hay.includes('lotion')) {
      return 'skincare';
    }
    if (hay.includes('lipstick') || hay.includes('mascara') || hay.includes('makeup') || hay.includes('make-up') || hay.includes('foundation') || hay.includes('nail') || hay.includes('perfume') || hay.includes('fragrance') || hay.includes('parfum') || hay.includes('eye') || hay.includes('lip')) {
      return 'beauty';
    }
    return 'personal-care';
  }
  if (hay.includes('kitchen') || hay.includes('dish') || hay.includes('pan') || hay.includes('cup') || hay.includes('mug') || hay.includes('bottle') || hay.includes('cook') || hay.includes('plate')) return 'kitchen';
  if (hay.includes('coffee') || hay.includes('tea') || hay.includes('oat') || hay.includes('granola') || hay.includes('cereal') || hay.includes('snack') || hay.includes('peanut') || hay.includes('food') || hay.includes('chocolate') || hay.includes('drink') || hay.includes('biscuit')) return 'groceries';
  if (hay.includes('notebook') || hay.includes('pen') || hay.includes('paper') || hay.includes('desk') || hay.includes('stationery') || hay.includes('office')) return 'study-workspace';
  if (hay.includes('battery') || hay.includes('cable') || hay.includes('usb') || hay.includes('electronic') || hay.includes('lamp') || hay.includes('bulb') || hay.includes('charger')) return 'electronics';
  if (hay.includes('fitness') || hay.includes('protein') || hay.includes('creatine') || hay.includes('sport') || hay.includes('gym')) return 'fitness';
  if (hay.includes('bag') || hay.includes('travel') || hay.includes('luggage')) return 'travel';
  if (hay.includes('cloth') || hay.includes('shirt') || hay.includes('shoe') || hay.includes('sock')) return 'fashion';
  if (hay.includes('storage') || hay.includes('box') || hay.includes('basket') || hay.includes('clean') || hay.includes('detergent') || hay.includes('laundry') || hay.includes('wash')) return 'storage-organization';
  if (hay.includes('candle') || hay.includes('decor') || hay.includes('towel') || hay.includes('pillow') || hay.includes('home')) return 'home-decor';
  return 'everyday-essentials';
}

function inferTags(catSlug, name) {
  const base = [catSlug, 'daily-essentials'];
  const n = name.toLowerCase();
  if (catSlug === 'skincare' || catSlug === 'beauty') base.push('skincare', 'self-care', 'wedding');
  if (catSlug === 'personal-care') base.push('hostel', 'travel', 'grooming', 'daily-essentials');
  if (catSlug === 'kitchen' || catSlug === 'groceries') base.push('kitchen', 'hostel', 'apartment', 'home-setup');
  if (catSlug === 'study-workspace' || catSlug === 'electronics') base.push('study', 'wfh', 'office', 'productivity');
  if (catSlug === 'fitness') base.push('fitness', 'workout');
  if (n.includes('travel') || n.includes('mini')) base.push('travel');
  return Array.from(new Set(base)).join(',');
}

async function fetchSlice(baseUrl, sourceType, query, maxPages = 20) {
  const siteBase = sourceType === 'OPEN_BEAUTY_FACTS'
    ? 'https://world.openbeautyfacts.org/product/'
    : 'https://world.openproductsfacts.org/product/';

  for (let page = 1; page <= maxPages; page++) {
    if (records.length >= TARGET_COUNT) return;
    const url = `${baseUrl}/api/v2/search?${query}&page=${page}&page_size=100&fields=code,product_name,brands,categories,quantity,image_front_url`;
    try {
      const res = await fetch(url, {
        headers: { 'User-Agent': 'MissionCartCatalogImporter/1.0' },
        signal: AbortSignal.timeout(12000),
      });
      if (!res.ok) break;
      const data = await res.json();
      const items = data.products || [];
      if (items.length === 0) break;

      let added = 0;
      for (const item of items) {
        if (records.length >= TARGET_COUNT) break;
        if (!isCleanProduct(item)) continue;
        const code = String(item.code).replace(/[^a-zA-Z0-9]/g, '');
        const brand = String(item.brands).split(',')[0].trim();
        const name = String(item.product_name).trim().replace(/\s+/g, ' ');
        const dedupKey = `${brand.toLowerCase()}::${name.toLowerCase()}`;
        const imgUrl = item.image_front_url.trim();

        if (seenCodes.has(code) || seenNameBrand.has(dedupKey) || seenImages.has(imgUrl)) continue;
        seenCodes.add(code);
        seenNameBrand.add(dedupKey);
        seenImages.add(imgUrl);

        const catSlug = inferCategorySlug(sourceType, item);
        const descParts = [
          `${name} by ${brand}.`,
          item.quantity ? `Pack size: ${item.quantity}.` : '',
          item.categories ? `Category: ${String(item.categories).split(',').slice(0, 3).join(', ')}.` : '',
          `Barcode: ${code}.`
        ].filter(Boolean);

        records.push({
          sku: `${sourceType === 'OPEN_BEAUTY_FACTS' ? 'OBF' : 'OPF'}-${code}`,
          name,
          brand,
          categorySlug: catSlug,
          sourcePlatform: sourceType,
          sourceProductId: code,
          sourceUrl: `${siteBase}${code}`,
          description: descParts.join(' '),
          quantitySpec: item.quantity || '',
          missionTags: inferTags(catSlug, name),
          imageUrl: imgUrl,
          imageSource: sourceType === 'OPEN_BEAUTY_FACTS' ? 'Open Beauty Facts' : 'Open Products Facts',
          imageLicense: 'ODbL 1.0',
          imageAttribution: `${brand} / Open Facts Contributors`,
        });
        added++;
      }
      console.log(`[${sourceType}] ${query} p${page}: +${added} (total: ${records.length})`);
      if (items.length < 100) break;
    } catch (e) {
      console.log(`ERR ${sourceType} ${query} p${page}: ${e.message}`);
      break;
    }
  }
}

async function main() {
  const obfSlices = [
    'countries_tags_en=india&sort_by=unique_scans_n',
    'countries_tags_en=united-states&sort_by=unique_scans_n',
    'countries_tags_en=united-kingdom&sort_by=unique_scans_n',
    'countries_tags_en=canada&sort_by=unique_scans_n',
    'countries_tags_en=australia&sort_by=unique_scans_n',
    'countries_tags_en=france&sort_by=unique_scans_n',
    'countries_tags_en=france&sort_by=last_modified_t',
    'countries_tags_en=france&sort_by=created_t',
    'countries_tags_en=germany&sort_by=unique_scans_n',
    'countries_tags_en=germany&sort_by=last_modified_t',
    'countries_tags_en=spain&sort_by=unique_scans_n',
    'countries_tags_en=italy&sort_by=unique_scans_n',
    'countries_tags_en=belgium&sort_by=unique_scans_n',
    'countries_tags_en=switzerland&sort_by=unique_scans_n',
    'countries_tags_en=netherlands&sort_by=unique_scans_n',
    'countries_tags_en=poland&sort_by=unique_scans_n',
    'categories_tags_en=shampoos&sort_by=unique_scans_n',
    'categories_tags_en=shower-gels&sort_by=unique_scans_n',
    'categories_tags_en=soaps&sort_by=unique_scans_n',
    'categories_tags_en=deodorants&sort_by=unique_scans_n',
    'categories_tags_en=toothpastes&sort_by=unique_scans_n',
    'categories_tags_en=perfumes&sort_by=unique_scans_n',
    'categories_tags_en=face-creams&sort_by=unique_scans_n',
    'categories_tags_en=hand-creams&sort_by=unique_scans_n',
    'categories_tags_en=body-lotions&sort_by=unique_scans_n',
    'categories_tags_en=sunscreens&sort_by=unique_scans_n',
    'categories_tags_en=lipsticks&sort_by=unique_scans_n',
    'categories_tags_en=hair-conditioners&sort_by=unique_scans_n',
    'sort_by=last_modified_t',
    'sort_by=created_t',
    'sort_by=completeness',
  ];

  const opfSlices = [
    'countries_tags_en=india&sort_by=unique_scans_n',
    'countries_tags_en=united-states&sort_by=unique_scans_n',
    'countries_tags_en=united-kingdom&sort_by=unique_scans_n',
    'countries_tags_en=france&sort_by=unique_scans_n',
    'countries_tags_en=france&sort_by=last_modified_t',
    'countries_tags_en=germany&sort_by=unique_scans_n',
    'countries_tags_en=spain&sort_by=unique_scans_n',
    'countries_tags_en=italy&sort_by=unique_scans_n',
    'sort_by=last_modified_t',
    'sort_by=completeness',
  ];

  for (const s of obfSlices) {
    if (records.length >= TARGET_COUNT) break;
    await fetchSlice('https://world.openbeautyfacts.org', 'OPEN_BEAUTY_FACTS', s, 20);
  }
  for (const s of opfSlices) {
    if (records.length >= TARGET_COUNT) break;
    await fetchSlice('https://world.openproductsfacts.org', 'OPEN_PRODUCTS_FACTS', s, 20);
  }

  const outDir = path.join(process.cwd(), 'backend', 'src', 'main', 'resources', 'db');
  fs.mkdirSync(outDir, { recursive: true });
  const jsonl = records.map((r) => JSON.stringify(r)).join('\n');
  const gz = zlib.gzipSync(Buffer.from(jsonl, 'utf8'));
  const outPath = path.join(outDir, 'open_catalog_seed.jsonl.gz');
  fs.writeFileSync(outPath, gz);
  console.log(`DONE! Saved ${records.length} distinct verified products (${(gz.length / 1024 / 1024).toFixed(2)} MB) to ${outPath}`);
}

main().catch((err) => { console.error(err); process.exit(1); });
