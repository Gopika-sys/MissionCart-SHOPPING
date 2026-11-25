import fs from 'node:fs';
import path from 'node:path';

const mediaPath = path.join(process.cwd(), 'scripts', 'curated-media.json');
const media = fs.existsSync(mediaPath) ? JSON.parse(fs.readFileSync(mediaPath, 'utf8')) : {};

function esc(s) {
  if (s === null || s === undefined) return 'NULL';
  return "'" + String(s).replace(/'/g, "''") + "'";
}

const categories = [
  [1, 'Electronics', 'electronics', 'Daily tech, chargers, lamps, and personal appliances', 'Work and study', 1],
  [2, 'Fashion', 'fashion', 'Apparel, footwear, bags, and accessories', 'Style and self-expression', 2],
  [3, 'Skincare', 'skincare', 'Cleansers, serums, moisturizers, and sun protection', 'Self-care', 3],
  [4, 'Beauty', 'beauty', 'Makeup, haircare, and personal beauty essentials', 'Self-care', 4],
  [5, 'Home Decor', 'home-decor', 'Ceramics, bedding, lighting, and textiles', 'Home and living', 5],
  [6, 'Kitchen', 'kitchen', 'Cookware, appliances, tableware, and meal prep', 'Home and living', 6],
  [7, 'Groceries', 'groceries', 'Coffee, tea, breakfast staples, and pantry basics', 'Everyday essentials', 7],
  [8, 'Travel', 'travel', 'Luggage, transit organizers, and travel adapters', 'Travel', 8],
  [9, 'Fitness', 'fitness', 'Yoga mats, weights, resistance bands, and recovery gear', 'Fitness', 9],
  [10, 'Study & Workspace', 'study-workspace', 'Laptop stands, notebooks, desk mats, and organizers', 'Work and study', 10],
  [11, 'Personal Care', 'personal-care', 'Towels, grooming trimmers, and travel hygiene kits', 'Everyday essentials', 11],
  [12, 'Storage & Organization', 'storage-organization', 'Baskets, closet hangers, and modular bins', 'New beginnings', 12],
  [13, 'Everyday Essentials', 'everyday-essentials', 'Versatile daily utilities, ambient lighting, and gifting', 'Everyday essentials', 13],
];

const brands = [
  [1, 'Philips Home', 'philips-home', 'Netherlands'],
  [2, 'Kora Living', 'kora-living', 'India'],
  [3, 'DailyObjects', 'dailyobjects', 'India'],
  [4, 'Nestasia', 'nestasia', 'India'],
  [5, 'Minimalist Lab', 'minimalist-lab', 'India'],
  [6, 'Bombay Shirt Co', 'bombay-shirt-co', 'India'],
  [7, 'JBL Audio', 'jbl-audio', 'USA'],
  [8, 'Anker Tech', 'anker-tech', 'USA'],
  [9, 'Solimo Basics', 'solimo-basics', 'India'],
  [10, 'FabIndia Craft', 'fabindia-craft', 'India'],
  [11, 'Plum Goodness', 'plum-goodness', 'India'],
  [12, 'Decathlon Domyos', 'decathlon-domyos', 'France'],
  [13, 'Mokobara Travel', 'mokobara-travel', 'India'],
  [14, 'Blue Tokai', 'blue-tokai', 'India'],
  [15, 'Logitech', 'logitech', 'Switzerland'],
  [16, 'Moleskine', 'moleskine', 'Italy'],
  [17, 'Wayfarer Eyewear', 'wayfarer-eyewear', 'Italy'],
  [18, 'Savana Edit', 'savana-edit', 'India'],
];

const retailers = [
  ['AMAZON_IN', 'Amazon India', 'https://www.amazon.in/', 'Broad General & Lifestyle', 'Electronics, Home appliances, Home furnishings, Storage, Kitchen essentials, Study accessories, Fitness, Personal care', 'Affiliate / PA-API 5.0 JSON', 'https://www.amazon.in/dp/{sourceProductId}', 'Amazon PA-API Verified Product Feed', 'CONFIGURED_FEED_PENDING', 0],
  ['FLIPKART', 'Flipkart', 'https://www.flipkart.com/', 'Broad General & Lifestyle', 'Electronics, Small appliances, Furniture, Home essentials, Fashion, Storage, Daily-use products', 'Flipkart Affiliate JSON Feed', 'https://www.flipkart.com/p/itm?pid={sourceProductId}', 'Authorized Flipkart Product Feed', 'CONFIGURED_FEED_PENDING', 0],
  ['MEESHO', 'Meesho', 'https://www.meesho.com/', 'Budget Home & Fashion', 'Budget-friendly home products, Storage organizers, Decor, Casual fashion, Kitchen accessories', 'CSV / Supplier Feed Import', 'https://www.meesho.com/s/p/{sourceProductId}', 'Supplier Verified Product Media', 'CONFIGURED_FEED_PENDING', 0],
  ['JIOMART', 'JioMart', 'https://www.jiomart.com/', 'Groceries & Household', 'Groceries, Household supplies, Food staples, Cleaning essentials, Kitchen products', 'Partner Catalog API / CSV', 'https://www.jiomart.com/p/{sourceProductId}', 'Authorized Catalog Asset Feed', 'CONFIGURED_FEED_PENDING', 0],
  ['TATA_CLIQ', 'Tata CLiQ', 'https://www.tatacliq.com/', 'Branded Lifestyle & Decor', 'Fashion, Home decor, Furnishings, Branded accessories, Personal gadgets', 'Partner Catalog JSON Feed', 'https://www.tatacliq.com/p-{sourceProductId}', 'Brand-Authorized Product Media', 'CONFIGURED_FEED_PENDING', 0],
  ['MYNTRA', 'Myntra', 'https://www.myntra.com/', 'Fashion, Footwear & Watches', 'Clothing, Shoes, Bags, Accessories, Watches, Beauty and grooming', 'Partner Fashion Feed', 'https://www.myntra.com/{sourceProductId}', 'Official Brand Lookbook / Product Feed', 'CONFIGURED_FEED_PENDING', 0],
  ['AJIO', 'AJIO', 'https://www.ajio.com/', 'Streetwear & Casual Fashion', 'Streetwear, Casual wear, Co-ord sets, Dresses, Footwear, Accessories', 'Partner Apparel Feed', 'https://www.ajio.com/p/{sourceProductId}', 'Partner Apparel Media Feed', 'CONFIGURED_FEED_PENDING', 0],
  ['SAVANA', 'Savana', 'https://www.savana.com/', 'Trend Apparel & Accessories', 'Trend-focused clothing, Co-ord outfits, Dresses, Tops, Denim, Bags, Jewellery', 'Trend Catalog JSON/CSV', 'https://www.savana.com/products/{sourceProductId}', 'Brand-Approved Product Shots', 'CONFIGURED_FEED_PENDING', 0],
  ['NYKAA', 'Nykaa', 'https://www.nykaa.com/', 'Skincare, Beauty & Wellness', 'Skincare, Haircare, Makeup, Beauty products, Fragrance, Wellness', 'Beauty Partner Feed / CSV', 'https://www.nykaa.com/p/{sourceProductId}', 'Manufacturer Skincare Assets', 'CONFIGURED_FEED_PENDING', 0],
  ['PURPLLE', 'Purplle', 'https://www.purplle.com/', 'Affordable Skincare & Beauty', 'Affordable skincare, Serums, Cleansers, Moisturizers, Makeup, Haircare, Grooming', 'Affiliate Beauty Feed', 'https://www.purplle.com/product/{sourceProductId}', 'Manufacturer Beauty Media', 'CONFIGURED_FEED_PENDING', 0],
  ['DAILYOBJECTS', 'DailyObjects', 'https://www.dailyobjects.com/', 'Tech, Desk & Work Bags', 'Desk mats, Tech organizers, Watch straps, Phone accessories, Laptop accessories, Bags', 'Shopify / Partner JSON Feed', 'https://www.dailyobjects.com/products/{sourceProductId}', 'Studio Product Photography', 'CONFIGURED_FEED_PENDING', 0],
  ['NESTASIA', 'Nestasia', 'https://nestasia.in/', 'Ceramics, Dining & Home Decor', 'Ceramics, Tableware, Vases, Dining products, Home decor, Furnishings, Kitchen accessories', 'Shopify / Partner JSON Feed', 'https://nestasia.in/products/{sourceProductId}', 'Brand Studio Tableware Photos', 'CONFIGURED_FEED_PENDING', 0],
  ['CROMA', 'Croma', 'https://www.croma.com/', 'Consumer Electronics & Appliances', 'Electronics, Small appliances, Laptops, Accessories, Home appliances', 'Authorized Electronics Feed', 'https://www.croma.com/p/{sourceProductId}', 'Manufacturer Electronics Media', 'CONFIGURED_FEED_PENDING', 0],
  ['MISSIONCART_DIRECT', 'MissionCart Direct', 'https://missioncart.local', 'Curated Purpose Bundles & Native Stock', 'Verified direct-stocked essentials across hostel, workspace, kitchen, skincare, and travel', 'Native Inventory', '/products/{sourceProductId}', 'Admin-Verified Studio Assets', 'ACTIVE', 1]
];

// 65 products across all 13 categories & all 13 retailers + MISSIONCART_DIRECT
const products = [
  // ELECTRONICS (cat 1)
  ['MC-ELEC-001', '1.5L Stainless Electric Kettle', 1, 1, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 1299, 1699, 'BUDGET', 'hostel,student,kitchen,apartment,daily-essentials', 'elec_kettle', { wattage: 1500, voltage: '230V', plugType: 'India Type D' }, ['REPAIR','EXCHANGE','RESELL'], 'Quick-boil 1500W stainless kettle with auto shut-off for hostel rooms and tea breaks.'],
  ['MC-ELEC-002', '6-Socket Surge Protector Power Strip', 1, 9, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 749, 999, 'BUDGET', 'hostel,student,study,wfh,remote,electronics', 'elec_powerstrip', { maxWattage: 1500, voltage: '230V', sockets: 6 }, ['EXCHANGE'], '6-outlet surge-protected extension strip rated up to 1500W for laptops, desk lamps, and phone chargers.'],
  ['MC-ELEC-003', 'Articulated Warm-Dim LED Desk Lamp', 1, 1, 'CROMA', 'EXTERNAL_CATALOG', 1499, 1999, 'BALANCED', 'study,student,wfh,remote,productivity,hostel', 'elec_lamp', { wattage: 9, voltage: '230V', colorTemp: '3000K-5000K' }, ['REPAIR','RESELL'], 'Flicker-free articulated study lamp with warm evening reading mode and glare shield.'],
  ['MC-ELEC-004', 'Over-Ear Wireless Focus Headphones', 1, 7, 'AMAZON_IN', 'EXTERNAL_CATALOG', 2499, 3499, 'BALANCED', 'study,wfh,remote,office,travel,fitness', 'elec_headphones', { batteryHours: 35, connector: 'USB-C', bluetooth: '5.3' }, ['REPAIR','RESELL'], 'Closed-back over-ear wireless headphones built for deep study sessions and remote calls.'],
  ['MC-ELEC-005', 'Compact Portable Bluetooth Speaker', 1, 7, 'FLIPKART', 'EXTERNAL_CATALOG', 2299, 2999, 'BALANCED', 'travel,event-preparation,party,home-setup', 'elec_speaker', { wattage: 16, batteryHours: 12, waterResistant: 'IPX7' }, ['RENT','RESELL'], 'Waterproof portable wireless speaker ideal for room ambiance and small gatherings.'],
  ['MC-ELEC-006', '2000W Portable Single Induction Cooktop', 1, 1, 'CROMA', 'EXTERNAL_CATALOG', 2399, 3299, 'BALANCED', 'kitchen,apartment,home-setup,student', 'elec_induction', { wattage: 2000, voltage: '230V', plugType: 'India 16A Type M' }, ['REPAIR','RESELL'], 'Energy-efficient 2000W touch induction stove with preset Indian cooking modes.'],
  ['MC-ELEC-007', 'Cordless Precision Beard & Hair Trimmer', 1, 1, 'AMAZON_IN', 'EXTERNAL_CATALOG', 1199, 1599, 'BUDGET', 'personal-care,wedding,office,travel,hostel', 'elec_trimmer', { batteryMins: 60, voltage: 'USB 5V' }, ['EXCHANGE'], 'Self-sharpening stainless blade trimmer with USB charging for grooming on the go.'],
  ['MC-ELEC-008', 'Wireless Ergonomic Optical Mouse', 1, 15, 'FLIPKART', 'EXTERNAL_CATALOG', 899, 1295, 'BUDGET', 'study,wfh,remote,productivity,office', 'elec_mouse', { dpi: 1600, wireless: '2.4GHz' }, ['EXCHANGE'], 'Contoured wireless mouse with silent clicks for libraries, hostels, and home offices.'],
  ['MC-ELEC-009', '10000mAh USB-C Fast Power Bank', 1, 8, 'CROMA', 'EXTERNAL_CATALOG', 1299, 1799, 'BALANCED', 'travel,manali,student,daily-essentials', 'elec_powerbank', { capacityMah: 10000, outputWatts: 20, connector: 'USB-C PD' }, ['EXCHANGE'], 'Slim 20W PD power bank that keeps smartphones and earbuds topped up during multi-day trips.'],
  ['MC-ELEC-010', 'US-Spec Travel Hair Dryer (110V Only Demo)', 1, 1, 'TATA_CLIQ', 'EXTERNAL_CATALOG', 1899, 2400, 'PREMIUM', 'travel,personal-care', null, { voltage: '110V', plugType: 'US Type A', wattage: 1200 }, ['EXCHANGE'], 'Compact imported travel dryer (110V US plug) — included to verify compatibility checks against 230V Indian sockets.'],

  // STUDY & WORKSPACE (cat 10)
  ['MC-STDY-001', 'Ventilated Aluminum Laptop Stand', 10, 3, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 999, 1499, 'BALANCED', 'study,wfh,remote,productivity,office,student', 'study_stand', { widthCm: 25, material: 'Anodized Aluminum', angleLevels: 6 }, ['RESELL'], 'Foldable anodized laptop riser elevating screens to ergonomic eye level.'],
  ['MC-STDY-002', 'Hardcover Ruled Study Journal (A5)', 10, 16, 'AMAZON_IN', 'EXTERNAL_CATALOG', 499, 699, 'BUDGET', 'study,student,office,productivity,event-preparation', 'study_notebook', { pages: 192, paperGsm: 100, size: 'A5' }, [], 'Thread-bound 100gsm ruled notebook for lecture notes, planning, and daily journaling.'],
  ['MC-STDY-003', 'Vegan Leather Extended Desk Mat', 10, 3, 'DAILYOBJECTS', 'EXTERNAL_CATALOG', 1199, 1599, 'BALANCED', 'study,wfh,remote,productivity,office', null, { widthCm: 80, depthCm: 40, material: 'Vegan Leather' }, ['RESELL'], '80x40cm water-resistant desk pad defining a clean tactile keyboard and mouse zone.'],
  ['MC-STDY-004', '6-in-1 USB-C Multiport Desk Hub', 10, 8, 'DAILYOBJECTS', 'EXTERNAL_CATALOG', 1699, 2299, 'BALANCED', 'study,wfh,remote,productivity,office', null, { connector: 'USB-C', ports: 'HDMI, 3xUSB-A, SD, PD' }, ['EXCHANGE'], 'Aluminum USB-C hub adding HDMI display output and high-speed USB ports.'],
  ['MC-STDY-005', 'Modular Timber Pen & Desk Organizer', 10, 4, 'NESTASIA', 'EXTERNAL_CATALOG', 649, 899, 'BUDGET', 'study,student,wfh,office', null, { widthCm: 22, compartments: 4 }, [], 'Minimalist desk caddy keeping pens, sticky notes, cables, and spectacles organized.'],

  // FASHION & ACCESSORIES (cat 2)
  ['MC-FASH-001', 'Crisp White Cotton Oxford Shirt', 2, 6, 'MYNTRA', 'EXTERNAL_CATALOG', 1499, 2199, 'BALANCED', 'office,job,formal,interview,wedding', null, { availableSizes: 'S,M,L,XL', fabric: '100% Cotton', fit: 'Tailored' }, ['EXCHANGE','RESELL'], 'Breathable tailored cotton button-down engineered for first-job wardrobes and smart layering.'],
  ['MC-FASH-002', 'Classic White Low-Top Sneakers', 2, 6, 'AJIO', 'EXTERNAL_CATALOG', 2199, 2999, 'BALANCED', 'travel,office,college,daily-essentials,casual', 'fash_sneakers', { availableSizes: 'UK6,UK7,UK8,UK9,UK10', upper: 'Leather' }, ['RESELL'], 'Clean minimalist court sneakers comfortable enough for full travel days and smart-casual Fridays.'],
  ['MC-FASH-003', 'Organic Heavyweight Canvas Tote Bag', 2, 3, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 549, 799, 'BUDGET', 'college,office,study,daily-essentials,event-preparation', 'fash_tote', { capacityLiters: 16, laptopFit: '15 inch' }, ['RESELL'], 'Reinforced cotton canvas tote carrying a 15-inch laptop, notebook, and water bottle.'],
  ['MC-FASH-004', 'Minimalist Quartz Leather Strap Watch', 2, 10, 'TATA_CLIQ', 'EXTERNAL_CATALOG', 1899, 2599, 'BALANCED', 'office,job,wedding,formal', 'fash_watch', { strap: 'Leather', waterResistance: '3ATM' }, ['REPAIR','RESELL'], 'Clean dial dress watch suited for office interviews, formal dinners, and weddings.'],
  ['MC-FASH-005', 'Classic Acetate Polarized Sunglasses', 2, 17, 'MYNTRA', 'EXTERNAL_CATALOG', 1299, 1999, 'BALANCED', 'travel,manali,wedding,outdoor', 'fash_sunglasses', { uvProtection: 'UV400', polarized: true }, ['RESELL'], 'UV400 polarized frames protecting eyes during mountain sun and daytime transit.'],
  ['MC-FASH-006', 'Padded 15.6-inch Commuter Laptop Backpack', 2, 3, 'DAILYOBJECTS', 'EXTERNAL_CATALOG', 1999, 2699, 'BALANCED', 'office,job,college,travel,study', null, { capacityLiters: 22, laptopFit: '15.6 inch' }, ['RESELL'], 'Water-repellent commuter backpack with suspended laptop sleeve and luggage strap.'],
  ['MC-FASH-007', 'Structured Charcoal Linen Blazer', 2, 6, 'TATA_CLIQ', 'EXTERNAL_CATALOG', 2999, 4499, 'PREMIUM', 'office,job,wedding,formal', null, { availableSizes: 'S,M,L,XL', fabric: 'Linen Blend' }, ['RENT','RESELL'], 'Lightweight structured blazer tailored for presentations, interviews, and evening receptions.'],
  ['MC-FASH-008', 'Tan Handcrafted Leather Loafers', 2, 6, 'MYNTRA', 'EXTERNAL_CATALOG', 1999, 2799, 'BALANCED', 'office,job,wedding,formal', null, { availableSizes: 'UK6,UK7,UK8,UK9,UK10', sole: 'TPR' }, ['EXCHANGE','RESELL'], 'Cushioned slip-on loafers pairing effortlessly with formal trousers and ethnic wear.'],
  ['MC-FASH-009', 'Handwoven Silk-Blend Festive Kurta', 2, 10, 'AJIO', 'EXTERNAL_CATALOG', 1899, 2699, 'BALANCED', 'wedding,festive,ethnic,event-preparation', null, { availableSizes: 'S,M,L,XL', fabric: 'Cotton Silk' }, ['RENT','EXCHANGE','RESELL'], 'Breathable artisanal festive kurta with subtle sheen for sangeet and wedding ceremonies.'],
  ['MC-FASH-010', 'Packable Thermal Fleece Puffer Jacket', 2, 12, 'AJIO', 'EXTERNAL_CATALOG', 1799, 2599, 'BALANCED', 'travel,manali,winter,mountain', null, { availableSizes: 'S,M,L,XL', warmthRating: '-5C to 12C' }, ['RENT','RESELL'], 'Lightweight insulated jacket that packs into its own pouch for Manali and winter getaways.'],
  ['MC-FASH-011', 'Ribbed Co-ord Knit Lounge Set', 2, 18, 'SAVANA', 'EXTERNAL_CATALOG', 1399, 1899, 'BALANCED', 'travel,wfh,casual,home-setup', null, { availableSizes: 'XS,S,M,L', fabric: 'Cotton Knit' }, ['EXCHANGE'], 'Soft breathable two-piece knit co-ord designed for flights and relaxed remote-work days.'],
  ['MC-FASH-012', 'Layered Gold-Plated Minimalist Jewellery Set', 2, 18, 'SAVANA', 'EXTERNAL_CATALOG', 699, 999, 'BUDGET', 'wedding,office,event-preparation', null, { finish: '18k Gold Plated', hypoallergenic: true }, [], 'Delicate everyday-to-festive necklace and hoop earring duo.'],

  // SKINCARE (cat 3)
  ['MC-SKIN-001', 'Gentle 2% Salicylic Gel Cleanser 150ml', 3, 5, 'NYKAA', 'EXTERNAL_CATALOG', 349, 449, 'BUDGET', 'skincare,oily,daily-essentials,hostel,travel', null, { actives: 'Salicylic Acid 2%, Zinc', skinType: 'Oily / Combination', step: 'Cleanser' }, [], 'pH-balanced foaming cleanser that clears excess sebum without leaving skin tight.'],
  ['MC-SKIN-002', 'Niacinamide 10% + Zinc Balancing Serum 30ml', 3, 5, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 549, 699, 'BALANCED', 'skincare,oily,daily-essentials', 'skin_dropper', { actives: 'Niacinamide 10%, Zinc 1%', skinType: 'Oily / Blemish-prone', step: 'Serum' }, [], 'Lightweight dropper serum formulated to visibly balance oiliness and support skin barrier clarity.'],
  ['MC-SKIN-003', 'Oil-Free Green Tea Gel Moisturizer 50ml', 3, 11, 'PURPLLE', 'EXTERNAL_CATALOG', 399, 499, 'BUDGET', 'skincare,oily,daily-essentials,travel', null, { actives: 'Green Tea, Hyaluronic Acid', skinType: 'Oily / Combination', step: 'Moisturizer' }, [], 'Non-comedogenic water-gel hydrator that absorbs cleanly in humid weather.'],
  ['MC-SKIN-004', 'Invisible Matte SPF 50 PA++++ Sunscreen 50g', 3, 5, 'NYKAA', 'EXTERNAL_CATALOG', 449, 549, 'BALANCED', 'skincare,oily,travel,manali,daily-essentials', null, { actives: 'UV Filters, Niacinamide', spf: '50 PA++++', step: 'Sunscreen' }, [], 'Zero-white-cast matte fluid sunscreen essential for morning routines and high-altitude travel.'],
  ['MC-SKIN-005', 'Overnight 0.3% Retinol Recovery Serum 30ml', 3, 5, 'PURPLLE', 'EXTERNAL_CATALOG', 599, 749, 'BALANCED', 'skincare,night-routine', null, { actives: 'Retinol 0.3%, Squalane', usage: 'Night only', step: 'Night Serum' }, [], 'Encapsulated evening serum — pair with non-acid nights and daytime SPF.'],
  ['MC-SKIN-006', 'Vitamin C 10% Brightening Morning Serum', 3, 11, 'NYKAA', 'EXTERNAL_CATALOG', 525, 650, 'BALANCED', 'skincare,morning-routine,wedding', null, { actives: 'Vitamin C (Ethyl Ascorbic Acid)', usage: 'Morning' }, [], 'Antioxidant morning serum designed to pair under broad-spectrum sunscreen.'],

  // BEAUTY (cat 4)
  ['MC-BEAU-001', 'Soothing Clay & Botanical Sheet Mask Trio', 4, 11, 'NYKAA', 'EXTERNAL_CATALOG', 299, 399, 'BUDGET', 'skincare,wedding,self-care,event-preparation', 'hero_skincare', { packSize: 3, skinType: 'All' }, [], 'Calming pre-event hydration masks for travel recovery and celebration prep.'],
  ['MC-BEAU-002', 'Tinted Peptide Lip & Cheek Balm', 4, 11, 'PURPLLE', 'EXTERNAL_CATALOG', 349, 449, 'BUDGET', 'travel,office,wedding,daily-essentials', null, { shade: 'Warm Terracotta', weightG: 12 }, [], 'Multi-use moisturizing tint in a compact pocket tube for quick desk-to-dinner touchups.'],
  ['MC-BEAU-003', 'Argan & Rosemary Frizz-Control Hair Serum', 4, 5, 'NYKAA', 'EXTERNAL_CATALOG', 429, 529, 'BALANCED', 'travel,manali,wedding,personal-care', null, { volumeMl: 50 }, [], 'Lightweight leave-in serum taming humidity frizz without weighing down hair.'],

  // HOME DECOR (cat 5)
  ['MC-HOME-001', 'Hand-Glazed Stoneware Bud Vase', 5, 4, 'NESTASIA', 'EXTERNAL_CATALOG', 799, 1099, 'BALANCED', 'home-setup,apartment,event-preparation,party,decor', 'home_vase', { heightCm: 18, material: 'Stoneware Ceramic' }, ['RESELL'], 'Sculptural matte-glazed ceramic vase elevating bookshelves, dining tables, and entry consoles.'],
  ['MC-HOME-002', '300-Thread-Count Breathable Cotton Bedsheet Set', 5, 2, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 1199, 1699, 'BALANCED', 'hostel,student,apartment,home-setup,daily-essentials', null, { size: 'Single / Twin (152x228cm)', includes: '1 Sheet + 2 Pillow Covers' }, ['EXCHANGE'], 'Soft pre-washed pure cotton bedding tailored for hostel single beds and guest rooms.'],
  ['MC-HOME-003', 'Woven Waffle-Knit Cotton Throw Blanket', 5, 2, 'TATA_CLIQ', 'EXTERNAL_CATALOG', 999, 1399, 'BALANCED', 'hostel,apartment,home-setup,wfh', null, { size: '130x180cm', material: 'Combed Cotton' }, ['RESELL'], 'Airy textured throw blanket for AC dorm rooms, reading corners, and sofa styling.'],
  ['MC-HOME-004', 'Linen Cushion Covers (Set of 2, Terracotta & Oat)', 5, 2, 'MEESHO', 'EXTERNAL_CATALOG', 449, 699, 'BUDGET', 'apartment,home-setup,party,decor', null, { size: '40x40cm', count: 2 }, [], 'Warm neutral textured cushion covers adding instant editorial depth to living spaces.'],
  ['MC-HOME-005', 'Warm Copper Fairy String Lights (10m)', 5, 9, 'MEESHO', 'EXTERNAL_CATALOG', 249, 399, 'BUDGET', 'party,birthday,event-preparation,hostel,decor', null, { lengthMeters: 10, power: 'USB / Plug', color: 'Warm White' }, [], 'Delicate warm-white micro LED string lights for birthday backdrops and cozy hostel walls.'],

  // KITCHEN (cat 6)
  ['MC-KIT-001', 'Artisanal Speckled Ceramic Coffee Mug', 6, 4, 'NESTASIA', 'EXTERNAL_CATALOG', 399, 549, 'BUDGET', 'study,wfh,hostel,kitchen,party,event-preparation', 'kit_mug', { capacityMl: 320, microwaveSafe: true }, [], 'Kiln-fired stoneware mug retaining warmth for morning coffee, masala chai, or hostel soup.'],
  ['MC-KIT-002', 'Seasoned Cast-Iron / Nonstick Frying Pan 24cm', 6, 9, 'AMAZON_IN', 'EXTERNAL_CATALOG', 1149, 1599, 'BALANCED', 'kitchen,apartment,home-setup', 'kit_pan', { diameterCm: 24, inductionCompatible: true }, ['RESELL'], 'Induction-compatible heavy-base skillet built for everyday breakfasts and shared apartment dinners.'],
  ['MC-KIT-003', 'Airtight Borosilicate Glass Storage Containers (Set of 3)', 6, 4, 'FLIPKART', 'EXTERNAL_CATALOG', 849, 1199, 'BALANCED', 'kitchen,apartment,home-setup,hostel,storage', null, { count: 3, capacityMl: '400, 650, 1000' }, [], 'Leakproof microwave-safe glass meal-prep containers with snap-lock lids.'],
  ['MC-KIT-004', '750ml Vacuum-Insulated Steel Water Bottle', 6, 9, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 599, 899, 'BUDGET', 'hostel,student,travel,manali,fitness,office', 'kit_bottle', { capacityMl: 750, insulationHours: 18 }, ['RESELL'], 'Double-wall stainless flask keeping water chilled for 18 hours or tea hot on mountain treks.'],
  ['MC-KIT-005', 'Stoneware Dinner & Snack Plates (Set of 4)', 6, 4, 'NESTASIA', 'EXTERNAL_CATALOG', 1299, 1799, 'BALANCED', 'kitchen,apartment,party,birthday,event-preparation', null, { count: 4, diameterCm: 23, material: 'Stoneware' }, [], 'Minimalist neutral stoneware plates suited for daily meals and hosting friends.'],

  // GROCERIES (cat 7)
  ['MC-GROC-001', 'Single-Estate Arabica Medium Roast Coffee (250g)', 7, 14, 'JIOMART', 'EXTERNAL_CATALOG', 449, 495, 'BALANCED', 'groceries,study,wfh,hostel,party,kitchen', 'groc_coffee', { weightG: 250, grind: 'Whole / Pour-over' }, [], 'Freshly roasted Chikmagalur arabica with notes of dark chocolate and toasted almond.'],
  ['MC-GROC-002', 'Whole-Grain Rolled Oats Pantry Jar (1kg)', 7, 9, 'JIOMART', 'EXTERNAL_CATALOG', 249, 320, 'BUDGET', 'groceries,hostel,fitness,breakfast,kitchen', 'groc_oats', { weightG: 1000, proteinPer100g: '13g' }, [], 'Quick-cooking high-fiber rolled oats for 3-minute hostel breakfasts and post-workout bowls.'],
  ['MC-GROC-003', 'Darjeeling Loose-Leaf Green Tea Tin (100g)', 7, 14, 'JIOMART', 'EXTERNAL_CATALOG', 329, 399, 'BUDGET', 'groceries,study,wfh,party,hostel', null, { weightG: 100, servings: 50 }, [], 'Antioxidant-rich first-flush green tea leaves for calm afternoon focus.'],
  ['MC-GROC-004', 'Roasted Trail Mix & Party Snack Assortment (400g)', 7, 9, 'JIOMART', 'EXTERNAL_CATALOG', 399, 499, 'BUDGET', 'groceries,travel,party,birthday,event-preparation,study', null, { weightG: 400 }, [], 'Almonds, cashews, cranberries, and spiced pumpkin seeds ready for travel or party bowls.'],

  // TRAVEL (cat 8)
  ['MC-TRAV-001', '20-Inch Polycarbonate Cabin Spinner Suitcase', 8, 13, 'TATA_CLIQ', 'EXTERNAL_CATALOG', 3499, 4999, 'BALANCED', 'travel,manali,trip,flight', null, { capacityLiters: 38, weightKg: 2.9, cabinApproved: true }, ['RENT','REPAIR','RESELL'], 'Lightweight hardshell cabin trolley with 360-degree silent wheels and TSA lock.'],
  ['MC-TRAV-002', 'Universal World Travel Plug Adapter (GaN USB-C)', 8, 8, 'AMAZON_IN', 'EXTERNAL_CATALOG', 899, 1299, 'BALANCED', 'travel,manali,electronics,trip', null, { plugSupport: 'IN/US/EU/UK', usbCPorts: 2 }, ['EXCHANGE'], 'All-in-one surge-safe travel adapter supporting 150+ countries plus dual USB charging.'],
  ['MC-TRAV-003', 'Memory Foam Washable Travel Neck Pillow', 8, 13, 'FLIPKART', 'EXTERNAL_CATALOG', 549, 799, 'BUDGET', 'travel,manali,trip,flight', null, { cover: 'Removable Cotton', compressible: true }, [], 'Ergonomic chin-support travel pillow packing compactly for overnight buses and flights.'],
  ['MC-TRAV-004', '6-Piece Compression Packing Cube Set', 8, 13, 'MEESHO', 'EXTERNAL_CATALOG', 499, 749, 'BUDGET', 'travel,manali,hostel,storage', null, { pieces: 6 }, [], 'Mesh-top breathable packing organizers separating warm layers, footwear, and cables.'],

  // FITNESS (cat 9)
  ['MC-FIT-001', '6mm High-Grip TPE Yoga & Exercise Mat', 9, 12, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 799, 1199, 'BUDGET', 'fitness,workout,home-workout,yoga,beginner', 'fit_mat', { thicknessMm: 6, lengthCm: 183, material: 'Non-slip TPE' }, ['RESELL'], 'Joint-cushioning reversible mat with carry strap for home workouts and stretching.'],
  ['MC-FIT-002', 'Neoprene Hex Dumbbells Pair (2 x 3 kg)', 9, 12, 'FLIPKART', 'EXTERNAL_CATALOG', 999, 1399, 'BALANCED', 'fitness,workout,home-workout,strength,beginner', 'fit_dumbbell', { weightKg: 6, pair: '2 x 3kg' }, ['RESELL'], 'Anti-roll hexagonal cast-iron dumbbells coated in floor-safe grip neoprene.'],
  ['MC-FIT-003', '3-Level Fabric Resistance Booty & Strength Bands', 9, 12, 'AMAZON_IN', 'EXTERNAL_CATALOG', 499, 699, 'BUDGET', 'fitness,workout,home-workout,travel,beginner', null, { levels: 'Light, Medium, Heavy' }, [], 'Non-slip woven resistance bands for warmups, glutes, and upper-body mobility.'],
  ['MC-FIT-004', 'Ball-Bearing Adjustable Speed Jump Rope', 9, 12, 'AMAZON_IN', 'EXTERNAL_CATALOG', 349, 499, 'BUDGET', 'fitness,workout,cardio,beginner', null, { lengthCm: 300, adjustable: true }, [], 'Tangle-free coated cable skipping rope for high-efficiency indoor cardio.'],

  // PERSONAL CARE (cat 11)
  ['MC-CARE-001', 'Combed Cotton Bath & Hand Towel Set (2 pcs)', 11, 2, 'MISSIONCART_DIRECT', 'MISSIONCART_DIRECT', 649, 899, 'BUDGET', 'hostel,student,apartment,home-setup,travel,daily-essentials', null, { gsm: 500, pieces: 2 }, [], 'Quick-drying 500 GSM pure cotton towels sized for hostel hooks and compact bathrooms.'],
  ['MC-CARE-002', 'Water-Resistant Hanging Toiletry Pouch', 11, 3, 'DAILYOBJECTS', 'EXTERNAL_CATALOG', 599, 849, 'BALANCED', 'hostel,travel,manali,fitness,personal-care', null, { compartments: 4, hookIncluded: true }, [], 'Fold-out travel and hostel shower caddy keeping skincare, toothbrush, and grooming neat.'],

  // STORAGE & ORGANIZATION (cat 12)
  ['MC-STOR-001', 'Handwoven Seagrass Storage Basket (Set of 2)', 12, 2, 'MEESHO', 'EXTERNAL_CATALOG', 649, 999, 'BUDGET', 'hostel,apartment,home-setup,storage,decor', null, { widthCm: 32, material: 'Natural Seagrass' }, [], 'Collapsible woven baskets for laundry, extra linens, or under-bed dorm organization.'],
  ['MC-STOR-002', 'Velvet Non-Slip Space-Saving Hangers (Pack of 12)', 12, 9, 'AMAZON_IN', 'EXTERNAL_CATALOG', 449, 699, 'BUDGET', 'hostel,apartment,office,storage', null, { count: 12, thicknessMm: 5 }, [], 'Ultra-slim flocked hangers preventing shirts and blazers from slipping in narrow wardrobes.'],
  ['MC-STOR-003', '3-Tier Rolling Narrow Utility Cart', 12, 9, 'FLIPKART', 'EXTERNAL_CATALOG', 1499, 2199, 'BALANCED', 'apartment,home-setup,kitchen,study,storage', null, { widthCm: 42, depthCm: 30, tiers: 3 }, ['RESELL'], 'Slim wheeled storage trolley fitting between desks, beds, or kitchen counters.'],

  // EVERYDAY ESSENTIALS (cat 13)
  ['MC-EVD-001', 'Hand-Poured Soy & Cedarwood Amber Jar Candle', 13, 2, 'NESTASIA', 'EXTERNAL_CATALOG', 499, 699, 'BALANCED', 'party,birthday,event-preparation,home-setup,gift', null, { burnHours: 36, wax: 'Natural Soy' }, [], 'Clean-burning scented candle that doubles as calming workspace ambiance or party return gift.'],
  ['MC-EVD-002', 'Insulated Lunch Box & Cutlery Pouch', 13, 9, 'MEESHO', 'EXTERNAL_CATALOG', 499, 749, 'BUDGET', 'office,college,daily-essentials,student', null, { containers: 3, leakproof: true }, [], 'Compact tiffin carrier with microwave-safe steel bowls and washable neoprene sleeve.']
];

const sql = [];
sql.push('-- MissionCart Seed Data (65 Products, 13 Categories, 13 Retailers + MissionCart Direct, Compatibility Rules)');
sql.push('');

for (const [id, name, slug, desc, pg, ord] of categories) {
  sql.push(`INSERT INTO categories (id, name, slug, description, purpose_group, display_order) VALUES (${id}, ${esc(name)}, ${esc(slug)}, ${esc(desc)}, ${esc(pg)}, ${ord});`);
}
sql.push('');

for (const [id, name, slug, country] of brands) {
  sql.push(`INSERT INTO brands (id, name, slug, country, is_verified) VALUES (${id}, ${esc(name)}, ${esc(slug)}, ${esc(country)}, TRUE);`);
}
sql.push('');

for (const [code, name, site, focus, cats, method, urlPat, imgMeth, status, isNative] of retailers) {
  sql.push(`INSERT INTO retailer_sources (code, name, official_website, category_focus, supported_categories, import_method, product_url_pattern, image_sourcing_method, connection_status, is_native_checkout) VALUES (${esc(code)}, ${esc(name)}, ${esc(site)}, ${esc(focus)}, ${esc(cats)}, ${esc(method)}, ${esc(urlPat)}, ${esc(imgMeth)}, ${esc(status)}, ${isNative ? 'TRUE' : 'FALSE'});`);
}
sql.push('');

let verifiedCount = 0;
let pendingImgCount = 0;

for (const p of products) {
  const [sku, name, catId, brandId, sourcePlatform, productType, price, origPrice, tier, tags, mediaKey, specs, lifecycle, desc] = p;
  const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') + '-' + sku.toLowerCase().replace(/[^a-z0-9]/g, '');
  const m = mediaKey ? media[mediaKey] : null;
  const imgUrl = m ? m.localPath : null;
  const imgSource = m ? m.source : null;
  const imgAttr = m ? m.attribution : null;
  const imgLic = m ? m.license : null;
  const imgStatus = m ? 'VERIFIED' : 'REQUIRES_IMAGE';
  if (m) verifiedCount++; else pendingImgCount++;

  const dataStatus = sourcePlatform === 'MISSIONCART_DIRECT' ? 'MISSIONCART_CATALOG_VERIFIED' : 'SAMPLE_IMPORT_PENDING';
  const stock = sourcePlatform === 'MISSIONCART_DIRECT' ? 24 : 12;

  sql.push(
    `INSERT INTO products (sku, name, slug, brand_id, category_id, source_platform, source_product_id, source_url, product_type, description, price, original_price, currency, price_verified, availability_status, stock_quantity, rating, review_count, rating_verified, data_verification_status, specifications_json, attributes_json, mission_tags, priority_tier, lifecycle_options_json, image_url, image_source, image_attribution, image_license, image_status, is_published, is_approved) VALUES (` +
    `${esc(sku)}, ${esc(name)}, ${esc(slug)}, ${brandId}, ${catId}, ${esc(sourcePlatform)}, ${esc(sku)}, NULL, ${esc(productType)}, ${esc(desc)}, ${price.toFixed(2)}, ${origPrice.toFixed(2)}, 'INR', FALSE, 'IN_STOCK', ${stock}, NULL, 0, FALSE, ${esc(dataStatus)}, ${esc(JSON.stringify(specs))}, ${esc(JSON.stringify(specs))}, ${esc(tags)}, ${esc(tier)}, ${esc(JSON.stringify(lifecycle))}, ${esc(imgUrl)}, ${esc(imgSource)}, ${esc(imgAttr)}, ${esc(imgLic)}, ${esc(imgStatus)}, TRUE, TRUE);`
  );
}
sql.push('');

// Compatibility Rules
const rules = [
  ['ELEC_VOLTAGE_IN', 'ELECTRONICS', 'Indian 230V Socket Voltage Check', 'Flags 110V/120V appliances incompatible with Indian 230V/50Hz outlets.', '{"requiredVoltage":"230V"}', 'INCOMPATIBLE', 'Use a step-down transformer or pick a 230V variant.'],
  ['ELEC_WATTAGE_SURGE', 'ELECTRONICS', 'Power Strip Load Safety Check', 'Prevents plugging combined >1500W appliances into standard 6A surge strips.', '{"maxStripWatts":1500}', 'INCOMPATIBLE', 'Plug 1500W-2000W heating appliances directly into a 16A wall socket.'],
  ['ELEC_PLUG_TYPE', 'ELECTRONICS', 'Plug Pin Compatibility', 'Warns when US/UK plugs are mixed without a travel adapter.', '{"plug":"Type D/M"}', 'WARNING', 'Add a universal travel adapter.'],
  ['FURN_SPACE_WIDTH', 'FURNITURE', 'Room & Desk Dimension Fit', 'Checks furniture/organizer width against stated room or desk dimensions.', '{"maxWidthCm":"context.availableWidthCm"}', 'INCOMPATIBLE', 'Select a narrower organizer or adjust desk clearance.'],
  ['FASH_SIZE_AVAIL', 'FASHION', 'Apparel & Footwear Size Match', 'Verifies requested size appears in product availableSizes attribute.', '{"sizeMatch":"context.size"}', 'WARNING', 'Confirm exact size on retailer page before purchase.'],
  ['SKIN_ACID_RETINOID', 'SKINCARE', 'AHA/BHA + Retinol Layering Caution', 'Flags using exfoliating acids and retinoids in the exact same evening step.', '{"conflict":["salicylic","retinol"]}', 'WARNING', 'Alternate nights and pair with morning SPF 50 sunscreen.']
];
for (const [code, dom, title, desc, cond, sev, act] of rules) {
  sql.push(`INSERT INTO compatibility_rules (rule_code, domain, title, description, condition_json, severity, recommended_action, is_active) VALUES (${esc(code)}, ${esc(dom)}, ${esc(title)}, ${esc(desc)}, ${esc(cond)}, ${esc(sev)}, ${esc(act)}, TRUE);`);
}
sql.push('');

sql.push(`INSERT INTO coupons (code, description, discount_type, discount_value, min_order_amount, max_discount_amount, is_active) VALUES ('WELCOME10', '10% off your first MissionCart native bundle (up to ₹300)', 'PERCENT', 10.00, 799.00, 300.00, TRUE);`);
sql.push(`INSERT INTO coupons (code, description, discount_type, discount_value, min_order_amount, max_discount_amount, is_active) VALUES ('MISSION250', 'Flat ₹250 off native orders above ₹2,000', 'FLAT', 250.00, 2000.00, 250.00, TRUE);`);

const outBackend = path.join(process.cwd(), 'backend', 'src', 'main', 'resources', 'db', 'migration', 'V2__seed_data.sql');
const outDbMig = path.join(process.cwd(), 'database', 'migrations', 'V1__init_schema.sql');
const outDbSeed = path.join(process.cwd(), 'database', 'seed', 'V2__seed_data.sql');
fs.mkdirSync(path.dirname(outDbMig), { recursive: true });
fs.mkdirSync(path.dirname(outDbSeed), { recursive: true });

fs.writeFileSync(outBackend, sql.join('\n'));
fs.copyFileSync(path.join(process.cwd(), 'backend', 'src', 'main', 'resources', 'db', 'migration', 'V1__init_schema.sql'), outDbMig);
fs.writeFileSync(outDbSeed, sql.join('\n'));

console.log(`Generated V2__seed_data.sql with ${products.length} products (${verifiedCount} VERIFIED photos, ${pendingImgCount} REQUIRES_IMAGE).`);
