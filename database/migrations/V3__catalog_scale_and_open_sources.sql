-- Expand retailer_sources with active open barcode datasets & add indexes for 10,000+ product queries

INSERT INTO retailer_sources (code, name, official_website, category_focus, supported_categories, import_method, product_url_pattern, image_sourcing_method, connection_status, is_native_checkout)
VALUES
('OPEN_BEAUTY_FACTS', 'Open Beauty Facts', 'https://world.openbeautyfacts.org', 'Verified Beauty, Skincare & Personal Care', 'Skincare, Beauty, Personal Care, Haircare, Fragrances', 'Open Beauty Facts API v2 (ODbL 1.0)', 'https://world.openbeautyfacts.org/product/{sourceProductId}', 'Verified Contributor Product Front Photos', 'ACTIVE_OPEN_DATASET', FALSE),
('OPEN_PRODUCTS_FACTS', 'Open Products Facts', 'https://world.openproductsfacts.org', 'Verified Household, Kitchen & Everyday Goods', 'Kitchen, Home Decor, Storage, Electronics, Stationery, Everyday Essentials', 'Open Products Facts API v2 (ODbL 1.0)', 'https://world.openproductsfacts.org/product/{sourceProductId}', 'Verified Contributor Product Front Photos', 'ACTIVE_OPEN_DATASET', FALSE);

CREATE INDEX idx_products_img_status ON products(image_status, is_published, is_approved);
CREATE INDEX idx_products_name_search ON products(name);
CREATE INDEX idx_products_avail ON products(availability_status);
