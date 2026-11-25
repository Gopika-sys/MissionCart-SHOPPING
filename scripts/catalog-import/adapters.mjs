// Source-specific normalization adapters for all 13 MissionCart partner ecosystems
export const SOURCE_ADAPTERS = {
  AMAZON_IN: {
    name: 'Amazon India',
    normalize: (raw) => ({
      sku: raw.sku || raw.asin,
      name: raw.title || raw.name,
      brand: raw.brand,
      categorySlug: mapCategory(raw.category || raw.categorySlug),
      sourcePlatform: 'AMAZON_IN',
      sourceProductId: raw.asin || raw.sourceProductId,
      sourceUrl: raw.product_url || raw.sourceUrl || null,
      productType: 'EXTERNAL_CATALOG',
      description: raw.description || raw.title,
      price: Number(raw.price || raw.selling_price || 0),
      originalPrice: raw.mrp ? Number(raw.mrp) : null,
      stockQuantity: Number(raw.stock ?? 10),
      missionTags: raw.tags || raw.missionTags || 'daily-essentials',
      priorityTier: raw.priorityTier || 'BALANCED',
      imageUrl: raw.image_url || raw.imageUrl || null,
      imageSource: raw.imageSource || 'Authorized Catalog Feed',
      imageLicense: raw.imageLicense || 'Feed Pending',
    }),
  },
  FLIPKART: {
    name: 'Flipkart',
    normalize: (raw) => defaultNormalize(raw, 'FLIPKART'),
  },
  MEESHO: {
    name: 'Meesho',
    normalize: (raw) => defaultNormalize({ ...raw, priorityTier: raw.priorityTier || 'BUDGET' }, 'MEESHO'),
  },
  JIOMART: {
    name: 'JioMart',
    normalize: (raw) => defaultNormalize(raw, 'JIOMART'),
  },
  TATA_CLIQ: {
    name: 'Tata CLiQ',
    normalize: (raw) => defaultNormalize(raw, 'TATA_CLIQ'),
  },
  MYNTRA: {
    name: 'Myntra',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'fashion' }, 'MYNTRA'),
  },
  AJIO: {
    name: 'AJIO',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'fashion' }, 'AJIO'),
  },
  SAVANA: {
    name: 'Savana',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'fashion' }, 'SAVANA'),
  },
  NYKAA: {
    name: 'Nykaa',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'skincare' }, 'NYKAA'),
  },
  PURPLLE: {
    name: 'Purplle',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'skincare' }, 'PURPLLE'),
  },
  DAILYOBJECTS: {
    name: 'DailyObjects',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'study-workspace' }, 'DAILYOBJECTS'),
  },
  NESTASIA: {
    name: 'Nestasia',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'home-decor' }, 'NESTASIA'),
  },
  CROMA: {
    name: 'Croma',
    normalize: (raw) => defaultNormalize({ ...raw, category: raw.category || 'electronics' }, 'CROMA'),
  },
  MISSIONCART_DIRECT: {
    name: 'MissionCart Direct',
    normalize: (raw) => ({ ...defaultNormalize(raw, 'MISSIONCART_DIRECT'), productType: 'MISSIONCART_DIRECT' }),
  },
};

function defaultNormalize(raw, sourceCode) {
  return {
    sku: raw.sku || raw.product_sku || '',
    name: raw.name || raw.title || '',
    brand: raw.brand || 'Unbranded',
    categorySlug: mapCategory(raw.categorySlug || raw.category),
    sourcePlatform: sourceCode,
    sourceProductId: raw.sourceProductId || raw.external_id || null,
    sourceUrl: raw.sourceUrl || raw.product_url || null,
    productType: raw.productType || 'EXTERNAL_CATALOG',
    description: raw.description || raw.name || '',
    price: Number(raw.price || raw.selling_price || 0),
    originalPrice: raw.originalPrice || raw.mrp ? Number(raw.originalPrice || raw.mrp) : null,
    stockQuantity: Number(raw.stockQuantity ?? raw.stock ?? 10),
    missionTags: raw.missionTags || raw.tags || 'daily-essentials',
    priorityTier: (raw.priorityTier || 'BALANCED').toUpperCase(),
    imageUrl: raw.imageUrl || raw.image_url || null,
    imageSource: raw.imageSource || 'Catalog Import',
    imageLicense: raw.imageLicense || 'Authorized Feed',
  };
}

function mapCategory(raw) {
  const s = (raw || 'everyday-essentials').toLowerCase().trim();
  if (s.includes('elect') || s.includes('tech')) return 'electronics';
  if (s.includes('fash') || s.includes('cloth') || s.includes('shoe')) return 'fashion';
  if (s.includes('skin')) return 'skincare';
  if (s.includes('beauty') || s.includes('makeup')) return 'beauty';
  if (s.includes('decor') || s.includes('home')) return 'home-decor';
  if (s.includes('kitch') || s.includes('cook')) return 'kitchen';
  if (s.includes('groc') || s.includes('food')) return 'groceries';
  if (s.includes('trav') || s.includes('lugg')) return 'travel';
  if (s.includes('fit') || s.includes('gym')) return 'fitness';
  if (s.includes('study') || s.includes('desk') || s.includes('work')) return 'study-workspace';
  if (s.includes('personal') || s.includes('groom')) return 'personal-care';
  if (s.includes('stor') || s.includes('organ')) return 'storage-organization';
  return 'everyday-essentials';
}
