package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.ai.ProductRetrievalService.RetrievalMatch;
import com.missioncart.compatibility.CompatibilityEngine;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import com.missioncart.compatibility.CompatibilityEngine.ProductCompatibilityResult;
import com.missioncart.missions.MissionItem;
import com.missioncart.possessions.PossessionService;
import com.missioncart.products.Product;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MissionBundleBuilder {

    private final PossessionService possessionService;
    private final CompatibilityEngine compatibilityEngine;

    public record BuiltBundle(
        List<MissionItem> items,
        List<String> unmatchedRequirements,
        CompatibilityReport compatibilityReport
    ) {}

    public MissionBundleBuilder(PossessionService possessionService, CompatibilityEngine compatibilityEngine) {
        this.possessionService = possessionService;
        this.compatibilityEngine = compatibilityEngine;
    }

    public BuiltBundle build(Long missionId, ParsedIntent intent, List<RetrievalMatch> matches, List<String> savedPossessions) {
        List<String> allOwned = new ArrayList<>();
        if (intent.existingPossessions() != null) allOwned.addAll(intent.existingPossessions());
        if (savedPossessions != null) allOwned.addAll(savedPossessions);

        List<MissionItem> items = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        List<Long> activeProductIds = new ArrayList<>();
        int idx = 0;

        for (RetrievalMatch match : matches) {
            if (match.product() == null) {
                unmatched.add(match.requirement().label());
                continue;
            }
            Product p = match.product();
            boolean owned = possessionService.matchesOwnedItem(p.getName(), p.getDescription(), match.requirement().categorySlug(), allOwned);

            MissionItem mi = new MissionItem();
            mi.setMissionId(missionId);
            mi.setProductId(p.getId());
            mi.setRequirementLabel(match.requirement().label());
            mi.setPriority(match.requirement().priority());
            mi.setQuantity(Math.max(1, match.requirement().quantity()));
            mi.setUnitPrice(p.getPrice());
            mi.setExcludedOwned(owned);
            mi.setSelected(!owned);
            mi.setRecommendationReason(owned
                ? "Excluded automatically because you indicated you already own a matching item."
                : match.requirement().reasoning());
            mi.setSortOrder(idx++);
            items.add(mi);
            if (!owned) activeProductIds.add(p.getId());
        }

        CompatibilityReport report = compatibilityEngine.evaluate(activeProductIds, intent.context());
        for (MissionItem item : items) {
            if (item.isExcludedOwned()) {
                item.setCompatibilityStatus("COMPATIBLE");
                item.setCompatibilityReason("Excluded (already owned)");
                continue;
            }
            ProductCompatibilityResult res = report.perProduct().get(item.getProductId());
            if (res != null) {
                item.setCompatibilityStatus(res.status());
                item.setCompatibilityReason(res.reason());
            }
        }

        return new BuiltBundle(items, unmatched, report);
    }
}
