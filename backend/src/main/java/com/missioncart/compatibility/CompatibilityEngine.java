package com.missioncart.compatibility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CompatibilityEngine {

    public record CompatibilityIssue(
        String ruleCode,
        String domain,
        String status,
        String reason,
        String recommendedAction,
        List<Long> affectedProductIds
    ) {}

    public record ProductCompatibilityResult(
        Long productId,
        String status,
        String reason
    ) {}

    public record CompatibilityReport(
        String overallStatus,
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct
    ) {}

    private final ProductRepository productRepo;
    private final CompatibilityRuleRepository ruleRepo;
    private final ObjectMapper objectMapper;

    public CompatibilityEngine(ProductRepository productRepo, CompatibilityRuleRepository ruleRepo, ObjectMapper objectMapper) {
        this.productRepo = productRepo;
        this.ruleRepo = ruleRepo;
        this.objectMapper = objectMapper;
    }

    public CompatibilityReport evaluate(List<Long> productIds, Map<String, Object> context) {
        if (productIds == null || productIds.isEmpty()) {
            return new CompatibilityReport("COMPATIBLE", List.of(), Map.of());
        }
        List<Product> products = productRepo.findAllById(productIds);
        Map<Long, Map<String, Object>> specsById = new HashMap<>();
        Map<Long, Map<String, Object>> attrsById = new HashMap<>();
        for (Product p : products) {
            specsById.put(p.getId(), parseMap(p.getSpecificationsJson()));
            attrsById.put(p.getId(), parseMap(p.getAttributesJson()));
        }

        List<CompatibilityIssue> issues = new ArrayList<>();
        Map<Long, ProductCompatibilityResult> perProduct = new LinkedHashMap<>();
        for (Product p : products) {
            perProduct.put(p.getId(), new ProductCompatibilityResult(p.getId(), "COMPATIBLE", "No conflicts detected"));
        }

        checkElectronics(products, specsById, attrsById, issues, perProduct);
        checkFurniture(products, specsById, attrsById, context, issues, perProduct);
        checkFashion(products, specsById, attrsById, context, issues, perProduct);
        checkSkincare(products, specsById, attrsById, issues, perProduct);

        String overall = "COMPATIBLE";
        for (CompatibilityIssue issue : issues) {
            if ("INCOMPATIBLE".equals(issue.status())) {
                overall = "INCOMPATIBLE";
                break;
            }
            if ("WARNING".equals(issue.status())) {
                overall = "WARNING";
            }
        }
        return new CompatibilityReport(overall, issues, perProduct);
    }

    private void checkElectronics(
        List<Product> products,
        Map<Long, Map<String, Object>> specsById,
        Map<Long, Map<String, Object>> attrsById,
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct
    ) {
        List<Product> highWattage = new ArrayList<>();
        List<Product> powerStrips = new ArrayList<>();

        for (Product p : products) {
            Map<String, Object> specs = specsById.get(p.getId());
            Map<String, Object> attrs = attrsById.get(p.getId());

            String voltage = str(specs.getOrDefault("voltage", attrs.get("voltage")));
            if (!voltage.isEmpty() && (voltage.contains("110") || voltage.contains("120")) && !voltage.contains("230") && !voltage.contains("240")) {
                addIssue(issues, perProduct, "ELEC_VOLTAGE_IN", "ELECTRONICS", "INCOMPATIBLE",
                    p.getName() + " specifies " + voltage + ", which is incompatible with standard Indian 230V/50Hz sockets without a step-down transformer.",
                    "Replace with a 220-240V India-compatible model or add a rated voltage converter.",
                    List.of(p.getId()));
            }

            String plug = str(specs.getOrDefault("plugType", attrs.get("plugType")));
            if (!plug.isEmpty() && (plug.contains("US") || plug.contains("Type A") || plug.contains("Type B")) && !plug.contains("Universal")) {
                addIssue(issues, perProduct, "ELEC_PLUG_TYPE", "ELECTRONICS", "WARNING",
                    p.getName() + " uses a " + plug + " plug that requires a Type D/M adapter for Indian wall outlets.",
                    "Add a Universal Travel Adapter to your mission or choose an Indian Type D/M variant.",
                    List.of(p.getId()));
            }

            Integer wattage = num(specs.getOrDefault("wattage", attrs.get("wattage")));
            Integer maxWatts = num(specs.getOrDefault("maxWattage", attrs.get("maxWattage")));
            if (maxWatts != null) {
                powerStrips.add(p);
            } else if (wattage != null && wattage >= 1000) {
                highWattage.add(p);
            }
        }

        if (!powerStrips.isEmpty() && !highWattage.isEmpty()) {
            for (Product strip : powerStrips) {
                Integer maxWatts = num(specsById.get(strip.getId()).getOrDefault("maxWattage", attrsById.get(strip.getId()).get("maxWattage")));
                int combinedWatts = highWattage.stream()
                    .mapToInt(h -> Optional.ofNullable(num(specsById.get(h.getId()).getOrDefault("wattage", attrsById.get(h.getId()).get("wattage")))).orElse(0))
                    .sum();
                if (maxWatts != null && combinedWatts > maxWatts) {
                    List<Long> ids = new ArrayList<>();
                    ids.add(strip.getId());
                    highWattage.forEach(h -> ids.add(h.getId()));
                    addIssue(issues, perProduct, "ELEC_WATTAGE_SURGE", "ELECTRONICS", "INCOMPATIBLE",
                        "Combined high-wattage load (" + combinedWatts + "W) exceeds " + strip.getName() + " rated limit (" + maxWatts + "W).",
                        "Plug high-wattage appliances (kettle/induction) directly into a 16A wall socket instead of the power strip, or pick a 16A heavy-duty strip.",
                        ids);
                }
            }
        }
    }

    private void checkFurniture(
        List<Product> products,
        Map<Long, Map<String, Object>> specsById,
        Map<Long, Map<String, Object>> attrsById,
        Map<String, Object> context,
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct
    ) {
        Integer maxSpaceCm = context != null ? num(context.get("availableWidthCm")) : null;
        for (Product p : products) {
            Map<String, Object> specs = specsById.get(p.getId());
            Integer widthCm = num(specs.getOrDefault("widthCm", attrsById.get(p.getId()).get("widthCm")));
            if (maxSpaceCm != null && widthCm != null && widthCm > maxSpaceCm) {
                addIssue(issues, perProduct, "FURN_SPACE_WIDTH", "FURNITURE", "INCOMPATIBLE",
                    p.getName() + " width (" + widthCm + " cm) exceeds your available space (" + maxSpaceCm + " cm).",
                    "Choose a more compact organizer or desk accessory.",
                    List.of(p.getId()));
            }
        }
    }

    private void checkFashion(
        List<Product> products,
        Map<Long, Map<String, Object>> specsById,
        Map<Long, Map<String, Object>> attrsById,
        Map<String, Object> context,
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct
    ) {
        String requestedSize = context != null ? str(context.get("size")).toUpperCase() : "";
        if (!requestedSize.isEmpty()) {
            for (Product p : products) {
                String availableSizes = str(attrsById.get(p.getId()).getOrDefault("availableSizes", specsById.get(p.getId()).get("availableSizes"))).toUpperCase();
                if (!availableSizes.isEmpty() && !availableSizes.contains(requestedSize)) {
                    addIssue(issues, perProduct, "FASH_SIZE_AVAIL", "FASHION", "WARNING",
                        p.getName() + " lists sizes (" + availableSizes + ") and may not include requested size " + requestedSize + ".",
                        "Verify size availability before checkout or select a comparable item.",
                        List.of(p.getId()));
                }
            }
        }
    }

    private void checkSkincare(
        List<Product> products,
        Map<Long, Map<String, Object>> specsById,
        Map<Long, Map<String, Object>> attrsById,
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct
    ) {
        List<Product> exfoliatingAcids = new ArrayList<>();
        List<Product> retinoids = new ArrayList<>();
        List<Product> vitaminC = new ArrayList<>();

        for (Product p : products) {
            String actives = (str(attrsById.get(p.getId()).get("actives")) + " " + p.getName() + " " + p.getDescription()).toLowerCase();
            if (actives.contains("salicylic") || actives.contains("glycolic") || actives.contains("aha") || actives.contains("bha")) {
                exfoliatingAcids.add(p);
            }
            if (actives.contains("retinol") || actives.contains("retinoid")) {
                retinoids.add(p);
            }
            if (actives.contains("vitamin c") || actives.contains("ascorbic")) {
                vitaminC.add(p);
            }
        }

        if (!exfoliatingAcids.isEmpty() && !retinoids.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            exfoliatingAcids.forEach(p -> ids.add(p.getId()));
            retinoids.forEach(p -> ids.add(p.getId()));
            addIssue(issues, perProduct, "SKIN_ACID_RETINOID", "SKINCARE", "WARNING",
                "Combining exfoliating acids (AHA/BHA) and retinol in the same night routine can increase skin sensitivity.",
                "Alternate nights (use AHA/BHA and retinol on separate evenings) and always pair with broad-spectrum SPF during the day.",
                ids);
        }

        if (!exfoliatingAcids.isEmpty() && !vitaminC.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            exfoliatingAcids.forEach(p -> ids.add(p.getId()));
            vitaminC.forEach(p -> ids.add(p.getId()));
            addIssue(issues, perProduct, "SKIN_VITC_ACID", "SKINCARE", "WARNING",
                "Layering Vitamin C serum directly with AHA/BHA acids in the same step may irritate sensitive skin.",
                "Apply Vitamin C in the morning routine before sunscreen, and reserve AHA/BHA for evening use.",
                ids);
        }
    }

    private void addIssue(
        List<CompatibilityIssue> issues,
        Map<Long, ProductCompatibilityResult> perProduct,
        String code,
        String domain,
        String status,
        String reason,
        String action,
        List<Long> affectedIds
    ) {
        issues.add(new CompatibilityIssue(code, domain, status, reason, action, affectedIds));
        for (Long id : affectedIds) {
            ProductCompatibilityResult current = perProduct.get(id);
            if (current == null || "COMPATIBLE".equals(current.status()) || ("WARNING".equals(current.status()) && "INCOMPATIBLE".equals(status))) {
                perProduct.put(id, new ProductCompatibilityResult(id, status, reason));
            }
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private Integer num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            String digits = String.valueOf(o).replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (Exception e) {
            return null;
        }
    }
}
