// MissionCart Catalog Importer CLI (JSON, CSV, API)
// Usage:
//   node scripts/catalog-import/importer.mjs --format json --source AMAZON_IN --file scripts/catalog-import/sample-amazon.json --preview
//   node scripts/catalog-import/importer.mjs --format csv --source NYKAA --file scripts/catalog-import/sample-nykaa.csv --token <ADMIN_JWT>
import fs from 'node:fs';
import { SOURCE_ADAPTERS } from './adapters.mjs';

function parseArgs(argv) {
  const out = {};
  for (let i = 2; i < argv.length; i++) {
    if (argv[i].startsWith('--')) {
      const k = argv[i].slice(2);
      const next = argv[i + 1];
      if (!next || next.startsWith('--')) out[k] = true;
      else { out[k] = next; i++; }
    }
  }
  return out;
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter((l) => l.trim().length > 0);
  if (lines.length < 2) return [];
  const headers = lines[0].split(',').map((h) => h.trim());
  return lines.slice(1).map((line) => {
    const vals = line.split(',').map((v) => v.trim());
    const obj = {};
    headers.forEach((h, idx) => { obj[h] = vals[idx] ?? ''; });
    return obj;
  });
}

export function validateAndNormalize(records, sourceCode) {
  const adapter = SOURCE_ADAPTERS[sourceCode] || SOURCE_ADAPTERS.MISSIONCART_DIRECT;
  const seenSkus = new Set();
  const valid = [];
  const rejected = [];

  records.forEach((raw, idx) => {
    const norm = adapter.normalize(raw);
    if (!norm.sku || norm.sku.length < 3) {
      rejected.push({ row: idx + 1, reason: 'Missing/invalid SKU', raw });
      return;
    }
    if (!norm.name) {
      rejected.push({ row: idx + 1, reason: 'Missing product name', raw });
      return;
    }
    if (!Number.isFinite(norm.price) || norm.price <= 0) {
      rejected.push({ row: idx + 1, reason: 'Price must be > 0 INR', raw });
      return;
    }
    const skuKey = norm.sku.toUpperCase();
    if (seenSkus.has(skuKey)) {
      rejected.push({ row: idx + 1, reason: `Duplicate SKU in batch: ${skuKey}`, raw });
      return;
    }
    seenSkus.add(skuKey);
    valid.push(norm);
  });

  return { valid, rejected };
}

async function main() {
  const args = parseArgs(process.argv);
  const format = (args.format || 'json').toLowerCase();
  const source = (args.source || 'MISSIONCART_DIRECT').toUpperCase();
  const filePath = args.file;
  const apiBase = args.api || 'http://localhost:8080/api';

  if (!filePath && !args.apiUrl) {
    console.error('Provide --file <path> or --apiUrl <endpoint>');
    process.exit(1);
  }

  let rawRecords = [];
  if (args.apiUrl) {
    const res = await fetch(args.apiUrl);
    rawRecords = await res.json();
  } else {
    const content = fs.readFileSync(filePath, 'utf8');
    rawRecords = format === 'csv' ? parseCsv(content) : JSON.parse(content);
  }

  const { valid, rejected } = validateAndNormalize(rawRecords, source);
  console.log(`Source: ${source} | Total: ${rawRecords.length} | Valid: ${valid.length} | Rejected: ${rejected.length}`);
  if (rejected.length > 0) console.log('Rejected rows:', rejected);

  if (args.preview || !args.token) {
    console.log('Preview mode (pass --token <ADMIN_JWT> without --preview to persist via backend API).');
    return;
  }

  const res = await fetch(`${apiBase}/admin/catalog/import`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${args.token}`,
    },
    body: JSON.stringify({
      sourcePlatform: source,
      format: 'JSON',
      fileName: filePath || args.apiUrl,
      content: JSON.stringify(valid),
      previewOnly: false,
      updateExisting: Boolean(args.update),
    }),
  });
  const body = await res.json();
  console.log('Import API response:', JSON.stringify(body, null, 2));
}

if (process.argv[1]?.endsWith('importer.mjs')) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
