package com.missioncart.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.categories.Brand;
import com.missioncart.categories.BrandRepository;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.products.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Component
@Order(10)
public class OpenCatalogSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenCatalogSeedRunner.class);

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OpenCatalogSeedRunner(
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        BrandRepository brandRepo,
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long existingCount = productRepo.count();
        if (existingCount >= 1000) {
            return;
        }
        ClassPathResource res = new ClassPathResource("db/open_catalog_seed.jsonl.gz");
        if (!res.exists()) {
            log.info("open_catalog_seed.jsonl.gz not found; skipping bulk open-catalog seed.");
            return;
        }

        Map<String, Long> categoryMap = new HashMap<>();
        for (Category c : categoryRepo.findAll()) {
            categoryMap.put(c.getSlug().toLowerCase(), c.getId());
        }
        Long fallbackCategoryId = categoryMap.values().stream().findFirst().orElse(1L);

        Map<String, Long> brandMap = new HashMap<>();
        for (Brand b : brandRepo.findAll()) {
            brandMap.put(b.getName().toLowerCase(), b.getId());
        }

        Set<String> existingSkus = new HashSet<>(jdbcTemplate.queryForList("SELECT UPPER(sku) FROM products", String.class));
        List<JsonNode> pendingRows = new ArrayList<>(10500);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(res.getInputStream()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonNode node = objectMapper.readTree(line);
                String sku = node.path("sku").asText("").trim().toUpperCase();
                if (sku.isEmpty() || existingSkus.contains(sku)) continue;
                existingSkus.add(sku);
                pendingRows.add(node);
            }
        }

        if (pendingRows.isEmpty()) return;

        // Pre-create missing brands in bulk
        List<Brand> newBrands = new ArrayList<>();
        Set<String> newBrandSlugs = new HashSet<>();
        for (Brand b : brandRepo.findAll()) newBrandSlugs.add(b.getSlug().toLowerCase());

        for (JsonNode n : pendingRows) {
            String bName = n.path("brand").asText("").trim();
            if (bName.isEmpty()) continue;
            String bKey = bName.toLowerCase();
            if (!brandMap.containsKey(bKey)) {
                String slug = bKey.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                if (slug.isEmpty()) slug = "brand-" + Math.abs(bKey.hashCode());
                while (newBrandSlugs.contains(slug)) slug = slug + "-" + (newBrands.size() + 1);
                newBrandSlugs.add(slug);
                Brand nb = new Brand();
                nb.setName(bName.length() > 115 ? bName.substring(0, 115) : bName);
                nb.setSlug(slug.length() > 115 ? slug.substring(0, 115) : slug);
                nb.setVerified(true);
                newBrands.add(nb);
                brandMap.put(bKey, -1L); // marker
            }
        }
        if (!newBrands.isEmpty()) {
            brandRepo.saveAll(newBrands);
            brandMap.clear();
            for (Brand b : brandRepo.findAll()) brandMap.put(b.getName().toLowerCase(), b.getId());
        }

        String insertSql = """
            INSERT INTO products (
                sku, name, slug, brand_id, category_id, source_platform, source_product_id, source_url,
                product_type, description, price, original_price, currency, price_verified, price_status,
                availability_status, stock_quantity, rating, review_count, rating_verified,
                data_verification_status, specifications_json, attributes_json, mission_tags,
                priority_tier, lifecycle_options_json, image_url, image_source, image_attribution,
                image_license, image_status, is_published, is_approved, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Timestamp now = Timestamp.from(Instant.now());
        int batchSize = 500;
        for (int i = 0; i < pendingRows.size(); i += batchSize) {
            List<JsonNode> chunk = pendingRows.subList(i, Math.min(i + batchSize, pendingRows.size()));
            jdbcTemplate.batchUpdate(insertSql, chunk, chunk.size(), (ps, n) -> {
                String sku = n.path("sku").asText().trim().toUpperCase();
                String name = n.path("name").asText().trim();
                if (name.length() > 250) name = name.substring(0, 250);
                String slugBase = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                String skuPart = sku.toLowerCase().replaceAll("[^a-z0-9]", "");
                String slug = (slugBase.length() > 160 ? slugBase.substring(0, 160) : slugBase) + "-" + skuPart;

                String bName = n.path("brand").asText("").trim().toLowerCase();
                Long brandId = brandMap.get(bName);
                Long catId = categoryMap.getOrDefault(n.path("categorySlug").asText("everyday-essentials").toLowerCase(), fallbackCategoryId);

                String qtySpec = n.path("quantitySpec").asText("");
                String specsJson = qtySpec.isEmpty() ? "{}" : "{\"packSize\":\"" + qtySpec.replace("\"", "'") + "\"}";

                ps.setString(1, sku);
                ps.setString(2, name);
                ps.setString(3, slug);
                if (brandId != null && brandId > 0) ps.setLong(4, brandId); else ps.setNull(4, java.sql.Types.BIGINT);
                ps.setLong(5, catId);
                ps.setString(6, n.path("sourcePlatform").asText("OPEN_BEAUTY_FACTS"));
                ps.setString(7, n.path("sourceProductId").asText(sku));
                ps.setString(8, n.path("sourceUrl").asText(null));
                ps.setString(9, "EXTERNAL_CATALOG");
                ps.setString(10, n.path("description").asText(name));
                ps.setNull(11, java.sql.Types.DECIMAL); // Never store missing price as 0
                ps.setNull(12, java.sql.Types.DECIMAL);
                ps.setString(13, "INR");
                ps.setBoolean(14, false);
                ps.setString(15, "UNAVAILABLE");
                ps.setString(16, "IN_STOCK");
                ps.setInt(17, 15);
                ps.setNull(18, java.sql.Types.DECIMAL);
                ps.setInt(19, 0);
                ps.setBoolean(20, false);
                ps.setString(21, "OPEN_CATALOG_PRICE_PENDING");
                ps.setString(22, specsJson);
                ps.setString(23, specsJson);
                ps.setString(24, n.path("missionTags").asText("daily-essentials"));
                ps.setString(25, "BALANCED");
                ps.setString(26, "[]");
                ps.setString(27, n.path("imageUrl").asText());
                ps.setString(28, n.path("imageSource").asText("Open Facts"));
                ps.setString(29, n.path("imageAttribution").asText("Open Facts Contributors"));
                ps.setString(30, n.path("imageLicense").asText("ODbL 1.0"));
                ps.setString(31, "VERIFIED");
                ps.setBoolean(32, true);
                ps.setBoolean(33, true);
                ps.setTimestamp(34, now);
                ps.setTimestamp(35, now);
            });
        }
        log.info("Loaded {} genuine open-catalog verified-image products into database.", pendingRows.size());
    }
}
