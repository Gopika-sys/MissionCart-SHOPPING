package com.missioncart.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.categories.Brand;
import com.missioncart.categories.BrandRepository;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogImportService {

    public record ImportRecordInput(
        String sku,
        String name,
        String brand,
        String categorySlug,
        String sourcePlatform,
        String sourceProductId,
        String sourceUrl,
        String productType,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stockQuantity,
        String missionTags,
        String priorityTier,
        String imageUrl,
        String imageSource,
        String imageLicense,
        String imageAttribution
    ) {}

    public record PreviewRow(
        int rowIndex,
        String sku,
        String name,
        String sourcePlatform,
        BigDecimal price,
        String status,
        String message
    ) {}

    public record ImportResult(
        Long jobId,
        boolean previewOnly,
        int totalRecords,
        int insertedCount,
        int updatedCount,
        int skippedDuplicateCount,
        int errorCount,
        int awaitingImageVerificationCount,
        List<PreviewRow> rows
    ) {}

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final CatalogImportJobRepository jobRepo;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public CatalogImportService(
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        BrandRepository brandRepo,
        CatalogImportJobRepository jobRepo,
        ObjectMapper objectMapper,
        JdbcTemplate jdbcTemplate,
        EntityManager entityManager
    ) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.jobRepo = jobRepo;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    public List<ImportRecordInput> parseJsonRecords(String jsonContent, String defaultSource) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(jsonContent, new TypeReference<>() {});
            return raw.stream().map(m -> normalizeMap(m, defaultSource)).toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON catalog payload: " + e.getMessage());
        }
    }

    public List<ImportRecordInput> parseCsvRecords(String csvContent, String defaultSource) {
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build()
            .parse(new StringReader(csvContent))) {
            List<ImportRecordInput> list = new ArrayList<>();
            for (CSVRecord rec : parser) {
                Map<String, Object> m = new HashMap<>();
                for (String h : parser.getHeaderNames()) {
                    m.put(h, rec.get(h));
                }
                list.add(normalizeMap(m, defaultSource));
            }
            return list;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CSV catalog payload: " + e.getMessage());
        }
    }

    @Transactional
    public ImportResult executeImport(
        List<ImportRecordInput> records,
        String sourcePlatform,
        String format,
        String fileName,
        boolean previewOnly,
        boolean updateExisting,
        String initiatedBy
    ) {
        List<PreviewRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> batchSkus = new HashSet<>();
        Set<String> existingSkus = new HashSet<>(jdbcTemplate.queryForList("SELECT UPPER(sku) FROM products", String.class));
        Map<String, Category> catBySlug = categoryRepo.findAll().stream()
            .collect(Collectors.toMap(c -> c.getSlug().toLowerCase(), c -> c, (a, b) -> a));
        Category fallbackCat = catBySlug.values().stream().findFirst().orElseThrow();
        Map<String, Brand> brandByName = brandRepo.findAll().stream()
            .collect(Collectors.toMap(b -> b.getName().toLowerCase(), b -> b, (a, b) -> a));

        int inserted = 0, updated = 0, skippedDup = 0, errorCount = 0, awaitingImg = 0;

        for (int i = 0; i < records.size(); i++) {
            ImportRecordInput r = records.get(i);
            int rowNum = i + 1;

            if (r.sku() == null || r.sku().isBlank() || r.sku().length() < 3) {
                errorCount++;
                String msg = "Invalid or missing SKU";
                errors.add("Row " + rowNum + ": " + msg);
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), "ERROR", msg));
                continue;
            }
            if (r.name() == null || r.name().isBlank()) {
                errorCount++;
                String msg = "Missing product name";
                errors.add("Row " + rowNum + ": " + msg);
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), "ERROR", msg));
                continue;
            }
            if (r.price() == null || r.price().compareTo(BigDecimal.ZERO) <= 0) {
                errorCount++;
                String msg = "Price must be greater than 0 INR";
                errors.add("Row " + rowNum + ": " + msg);
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), "ERROR", msg));
                continue;
            }
            String skuUpper = r.sku().trim().toUpperCase();
            if (!batchSkus.add(skuUpper)) {
                skippedDup++;
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), "DUPLICATE_IN_BATCH", "Duplicate SKU inside upload batch"));
                continue;
            }

            boolean existsInDb = existingSkus.contains(skuUpper);
            Product existingEntity = null;
            if (existsInDb && updateExisting) {
                existingEntity = productRepo.findBySkuIgnoreCase(skuUpper).orElse(null);
            } else if (!existsInDb && r.sourceProductId() != null && !r.sourceProductId().isBlank()) {
                existingEntity = productRepo.findBySourcePlatformAndSourceProductId(r.sourcePlatform(), r.sourceProductId()).orElse(null);
                if (existingEntity != null) existsInDb = true;
            }

            if (existsInDb && !updateExisting) {
                skippedDup++;
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), "SKIPPED_DUPLICATE", "Existing record matched SKU or sourceProductId"));
                continue;
            }

            boolean validImageUrl = r.imageUrl() != null && (r.imageUrl().startsWith("https://") || r.imageUrl().startsWith("http://") || r.imageUrl().startsWith("/"));
            String imgStatus = validImageUrl ? "PENDING_REVIEW" : "REQUIRES_IMAGE";
            awaitingImg++;

            if (!previewOnly) {
                Product p = existingEntity != null ? existingEntity : new Product();
                boolean isNew = p.getId() == null;
                p.setSku(skuUpper);
                p.setName(r.name().trim());
                if (isNew) {
                    p.setSlug(slugify(r.name()) + "-" + skuUpper.toLowerCase().replaceAll("[^a-z0-9]", ""));
                }
                Category cat = catBySlug.getOrDefault(r.categorySlug() != null ? r.categorySlug().trim().toLowerCase() : "everyday-essentials", fallbackCat);
                p.setCategoryId(cat.getId());

                if (r.brand() != null && !r.brand().isBlank()) {
                    String bKey = r.brand().trim().toLowerCase();
                    Brand b = brandByName.get(bKey);
                    if (b == null) {
                        Brand nb = new Brand();
                        nb.setName(r.brand().trim());
                        nb.setSlug(slugify(r.brand().trim()) + "-" + Math.abs(bKey.hashCode()));
                        b = brandRepo.save(nb);
                        brandByName.put(bKey, b);
                    }
                    p.setBrandId(b.getId());
                }

                p.setSourcePlatform(r.sourcePlatform() != null && !r.sourcePlatform().isBlank() ? r.sourcePlatform() : sourcePlatform);
                p.setSourceProductId(r.sourceProductId());
                p.setSourceUrl(r.sourceUrl());
                p.setProductType(r.productType() != null && !r.productType().isBlank() ? r.productType() : "EXTERNAL_CATALOG");
                p.setDescription(r.description() != null && !r.description().isBlank() ? r.description() : r.name());
                p.setPrice(r.price());
                p.setOriginalPrice(r.originalPrice());
                p.setCurrency("INR");
                p.setStockQuantity(r.stockQuantity() != null ? Math.max(0, r.stockQuantity()) : 10);
                p.setAvailabilityStatus(p.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
                p.setMissionTags(r.missionTags());
                p.setPriorityTier(r.priorityTier() != null && !r.priorityTier().isBlank() ? r.priorityTier().toUpperCase() : "BALANCED");
                if (validImageUrl) {
                    p.setImageUrl(r.imageUrl());
                    p.setImageSource(r.imageSource());
                    p.setImageLicense(r.imageLicense());
                    p.setImageAttribution(r.imageAttribution());
                }
                p.setImageStatus(imgStatus);
                p.setDataVerificationStatus("IMPORTED_FEED_RECORD");
                p.setPublished(true);
                p.setApproved(true);
                productRepo.save(p);
                existingSkus.add(skuUpper);

                if (isNew) inserted++; else updated++;
                if ((inserted + updated) % 250 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), p.getSourcePlatform(), r.price(), isNew ? "INSERTED" : "UPDATED", "Image status: " + imgStatus));
            } else {
                if (existsInDb) updated++; else inserted++;
                if (rows.size() < 200) rows.add(new PreviewRow(rowNum, r.sku(), r.name(), r.sourcePlatform(), r.price(), existsInDb ? "WILL_UPDATE" : "WILL_INSERT", "Valid (Image: " + imgStatus + ")"));
            }
        }

        Long jobId = null;
        if (!previewOnly) {
            CatalogImportJob job = new CatalogImportJob();
            job.setSourcePlatform(sourcePlatform != null ? sourcePlatform : "MIXED");
            job.setFormat(format);
            job.setFileName(fileName);
            job.setStatus(errorCount > 0 && inserted == 0 && updated == 0 ? "FAILED" : "COMPLETED");
            job.setTotalRecords(records.size());
            job.setInsertedCount(inserted);
            job.setUpdatedCount(updated);
            job.setSkippedDuplicateCount(skippedDup);
            job.setErrorCount(errorCount);
            job.setInitiatedBy(initiatedBy != null ? initiatedBy : "system");
            job.setCompletedAt(Instant.now());
            try { job.setErrorsJson(objectMapper.writeValueAsString(errors.stream().limit(100).toList())); } catch (Exception ignored) {}
            jobId = jobRepo.save(job).getId();
        }

        return new ImportResult(jobId, previewOnly, records.size(), inserted, updated, skippedDup, errorCount, awaitingImg, rows);
    }

    private ImportRecordInput normalizeMap(Map<String, Object> m, String defaultSource) {
        String sku = str(m.getOrDefault("sku", m.getOrDefault("product_sku", m.get("asin"))));
        String name = str(m.getOrDefault("name", m.getOrDefault("title", m.get("product_name"))));
        String brand = str(m.getOrDefault("brand", m.get("manufacturer")));
        String cat = str(m.getOrDefault("categorySlug", m.getOrDefault("category", "everyday-essentials")));
        String source = str(m.getOrDefault("sourcePlatform", m.getOrDefault("source", defaultSource)));
        String srcId = str(m.getOrDefault("sourceProductId", m.get("external_id")));
        String srcUrl = str(m.getOrDefault("sourceUrl", m.get("product_url")));
        String type = str(m.getOrDefault("productType", "MISSIONCART_DIRECT".equalsIgnoreCase(source) ? "MISSIONCART_DIRECT" : "EXTERNAL_CATALOG"));
        String desc = str(m.getOrDefault("description", name));
        BigDecimal price = dec(m.getOrDefault("price", m.get("selling_price")));
        BigDecimal orig = dec(m.getOrDefault("originalPrice", m.get("mrp")));
        Integer stock = num(m.getOrDefault("stockQuantity", m.get("stock")));
        String tags = str(m.getOrDefault("missionTags", m.get("tags")));
        String tier = str(m.getOrDefault("priorityTier", "BALANCED"));
        String img = str(m.getOrDefault("imageUrl", m.get("image_url")));
        String imgSrc = str(m.getOrDefault("imageSource", "Catalog Import"));
        String imgLic = str(m.getOrDefault("imageLicense", "Authorized Feed"));
        String imgAttr = str(m.getOrDefault("imageAttribution", source));

        return new ImportRecordInput(sku, name, brand, cat, source, srcId, srcUrl, type, desc, price, orig, stock, tags, tier, img, imgSrc, imgLic, imgAttr);
    }

    private String str(Object o) { return o == null ? null : String.valueOf(o).trim(); }
    private BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        try { return new BigDecimal(String.valueOf(o).replaceAll("[^0-9.]", "")); } catch (Exception e) { return null; }
    }
    private Integer num(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        try { return Integer.parseInt(String.valueOf(o).replaceAll("[^0-9]", "")); } catch (Exception e) { return null; }
    }
    private String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
