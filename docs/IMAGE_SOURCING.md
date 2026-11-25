# Product Image Sourcing & Verification

1. **Zero Fake / Placeholder Customer Cards:** Public customer-facing endpoints (`GET /api/products`, `GET /api/products/search`) default to `verifiedImagesOnly=true` so products without verified photographs remain in the Admin Image-Review Queue (`/admin/images`) and never display `"PHOTOGRAPH PENDING VERIFICATION"` on storefront cards.
2. **Verified Curated Assets:** Local open-license product & editorial photographs from Wikimedia Commons (`frontend/public/verified-media/`) with exact `imageSource`, `imageLicense`, and `imageAttribution`.
3. **Verified Open-Catalog Product Photos:** Open Beauty Facts (`images.openbeautyfacts.org`) and Open Products Facts (`images.openproductsfacts.org`) front product photographs paired 1:1 with unique barcodes (never repeated across unrelated products).
4. **Referrer Policy:** `frontend/index.html` includes `<meta name="referrer" content="no-referrer" />` and `ProductCard` / `ProductDetailPage` pass `referrerPolicy="no-referrer"` with `object-fit: contain` for clean full-product framing.
