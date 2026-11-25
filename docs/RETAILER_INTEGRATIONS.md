# Retailer Source Integrations & Open Catalog Datasets

MissionCart models 13 external Indian retail platforms plus `MISSIONCART_DIRECT` and two active Open Facts barcode datasets in `retailer_sources` and `scripts/catalog-import/adapters.mjs`:

## 13 External Indian Retailer Ecosystems
1. Amazon India (`AMAZON_IN`)
2. Flipkart (`FLIPKART`)
3. Meesho (`MEESHO`)
4. JioMart (`JIOMART`)
5. Tata CLiQ (`TATA_CLIQ`)
6. Myntra (`MYNTRA`)
7. AJIO (`AJIO`)
8. Savana (`SAVANA`)
9. Nykaa (`NYKAA`)
10. Purplle (`PURPLLE`)
11. DailyObjects (`DAILYOBJECTS`)
12. Nestasia (`NESTASIA`)
13. Croma (`CROMA`)

## Active Open Catalog Sources (ODbL 1.0)
- **Open Beauty Facts (`OPEN_BEAUTY_FACTS`)**: `ACTIVE_OPEN_DATASET` — real skincare, beauty, haircare, fragrance, and personal-care products with verified barcodes, brands, product URLs, and front product photographs.
- **Open Products Facts (`OPEN_PRODUCTS_FACTS`)**: `ACTIVE_OPEN_DATASET` — real household, kitchen, storage, stationery, and everyday utility products with verified barcodes, brands, product URLs, and front product photographs.

## Honest Integration Policy
- External Indian retailer feeds without live affiliate/API credentials remain marked `CONFIGURED_FEED_PENDING` with sample records clearly labeled `SAMPLE_IMPORT_PENDING`.
- Open-dataset records without live Indian retailer pricing are stored with `price_verified = FALSE` (`OPEN_CATALOG_PRICE_PENDING`) rather than inventing synthetic INR prices.
- Native MissionCart checkout only processes `MISSIONCART_DIRECT` and `SELLER_MANAGED` SKUs.
