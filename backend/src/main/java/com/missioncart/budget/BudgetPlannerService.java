package com.missioncart.budget;

import com.missioncart.missions.MissionItem;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BudgetPlannerService {

    public record BudgetSummary(
        BigDecimal totalBudget,
        BigDecimal essentialSpending,
        BigDecimal optionalSpending,
        BigDecimal productSubtotal,
        BigDecimal totalSelected,
        BigDecimal verifiedDiscount,
        BigDecimal shippingCost,
        BigDecimal applicableTaxes,
        BigDecimal couponDiscount,
        BigDecimal finalPayable,
        BigDecimal remainingBudget,
        double utilizationPercent,
        BigDecimal excludedOwnedSavings,
        int unpricedItemCount,
        boolean overBudget,
        boolean affordableConfirmed
    ) {}

    public record OptimizationSuggestion(
        Long missionItemId,
        String currentProductName,
        BigDecimal currentPrice,
        String actionType,
        Long suggestedProductId,
        String suggestedProductName,
        BigDecimal suggestedPrice,
        BigDecimal savings,
        String reason
    ) {}

    public record OptimizationPlan(
        BudgetSummary currentBudget,
        BudgetSummary projectedBudget,
        BigDecimal totalPotentialSavings,
        List<OptimizationSuggestion> suggestions
    ) {}

    private final ProductRepository productRepo;

    public BudgetPlannerService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public boolean isPricedAndValid(Product p) {
        if (p == null || p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0) return false;
        String st = p.getPriceStatus();
        return !"UNAVAILABLE".equalsIgnoreCase(st) && !"EXPIRED".equalsIgnoreCase(st);
    }

    public BudgetSummary calculate(BigDecimal budget, List<MissionItem> items, Map<Long, Product> productMap) {
        return calculateWithCoupon(budget, items, productMap, BigDecimal.ZERO);
    }

    public BudgetSummary calculateWithCoupon(BigDecimal budget, List<MissionItem> items, Map<Long, Product> productMap, BigDecimal couponDiscount) {
        BigDecimal essential = BigDecimal.ZERO;
        BigDecimal optional = BigDecimal.ZERO;
        BigDecimal ownedSavings = BigDecimal.ZERO;
        BigDecimal verifiedDiscount = BigDecimal.ZERO;
        BigDecimal taxes = BigDecimal.ZERO;
        int unpricedCount = 0;

        Set<Long> seenProductIds = new HashSet<>();

        for (MissionItem item : items) {
            if (!seenProductIds.add(item.getProductId())) {
                continue; // Never double-count the same product
            }
            int qty = Math.max(1, item.getQuantity());
            Product p = productMap.get(item.getProductId());
            boolean hasValidPrice = p != null ? isPricedAndValid(p) : (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);

            if (item.isExcludedOwned()) {
                if (hasValidPrice) {
                    BigDecimal unit = p != null ? p.getPrice() : item.getUnitPrice();
                    ownedSavings = ownedSavings.add(unit.multiply(BigDecimal.valueOf(qty)));
                }
                continue;
            }
            if (!item.isSelected()) continue;

            if (!hasValidPrice) {
                unpricedCount++;
                continue; // Never treat missing/expired price as Rs 0 or include in verified total
            }

            BigDecimal unit = p != null ? p.getPrice() : item.getUnitPrice();
            BigDecimal line = unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            if ("ESSENTIAL".equalsIgnoreCase(item.getPriority())) {
                essential = essential.add(line);
            } else {
                optional = optional.add(line);
            }

            if (p != null && p.isPriceVerified() && p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(unit) > 0) {
                verifiedDiscount = verifiedDiscount.add(p.getOriginalPrice().subtract(unit).multiply(BigDecimal.valueOf(qty)));
            }

            BigDecimal rate = (p != null && p.getTaxRate() != null) ? p.getTaxRate() : BigDecimal.valueOf(18);
            BigDecimal lineTax = line.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            taxes = taxes.add(lineTax);
        }

        BigDecimal subtotal = essential.add(optional).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.valueOf(999)) < 0
            ? BigDecimal.valueOf(79.00).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal safeCoupon = couponDiscount != null ? couponDiscount.min(subtotal) : BigDecimal.ZERO;
        BigDecimal finalPayable = subtotal.add(shipping).add(taxes).subtract(safeCoupon).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal safeBudget = budget != null && budget.compareTo(BigDecimal.ZERO) > 0 ? budget.setScale(2, RoundingMode.HALF_UP) : finalPayable;
        BigDecimal remaining = safeBudget.subtract(finalPayable).setScale(2, RoundingMode.HALF_UP);
        double utilization = safeBudget.compareTo(BigDecimal.ZERO) > 0
            ? finalPayable.multiply(BigDecimal.valueOf(100)).divide(safeBudget, 1, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        boolean overBudget = budget != null && budget.compareTo(BigDecimal.ZERO) > 0 && finalPayable.compareTo(budget) > 0;
        boolean affordableConfirmed = !overBudget && unpricedCount == 0 && subtotal.compareTo(BigDecimal.ZERO) > 0;

        return new BudgetSummary(
            safeBudget,
            essential.setScale(2, RoundingMode.HALF_UP),
            optional.setScale(2, RoundingMode.HALF_UP),
            subtotal,
            finalPayable,
            verifiedDiscount.setScale(2, RoundingMode.HALF_UP),
            shipping,
            taxes,
            safeCoupon.setScale(2, RoundingMode.HALF_UP),
            finalPayable,
            remaining,
            utilization,
            ownedSavings.setScale(2, RoundingMode.HALF_UP),
            unpricedCount,
            overBudget,
            affordableConfirmed
        );
    }

    public OptimizationPlan suggestOptimizations(BigDecimal budget, List<MissionItem> items) {
        Map<Long, Product> productMap = new HashMap<>();
        productRepo.findAllById(items.stream().map(MissionItem::getProductId).toList())
            .forEach(p -> productMap.put(p.getId(), p));

        BudgetSummary current = calculate(budget, items, productMap);
        if (!current.overBudget()) {
            return new OptimizationPlan(current, current, BigDecimal.ZERO, List.of());
        }

        BigDecimal excess = current.finalPayable().subtract(current.totalBudget());
        BigDecimal accumulatedSavings = BigDecimal.ZERO;
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        for (MissionItem item : items) {
            if (!item.isSelected() || item.isExcludedOwned()) continue;
            Product currentProd = productMap.get(item.getProductId());
            if (currentProd == null || !isPricedAndValid(currentProd)) continue;

            List<Product> cheaper = productRepo.findByCategoryIdAndPublishedTrueAndApprovedTrueAndIdNot(currentProd.getCategoryId(), currentProd.getId())
                .stream()
                .filter(p -> isPricedAndValid(p) && p.getPrice().compareTo(currentProd.getPrice()) < 0)
                .sorted(Comparator.comparing(Product::getPrice))
                .toList();

            if (!cheaper.isEmpty()) {
                Product best = cheaper.get(0);
                BigDecimal diff = currentProd.getPrice().subtract(best.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity()));
                suggestions.add(new OptimizationSuggestion(
                    item.getId(),
                    currentProd.getName(),
                    currentProd.getPrice(),
                    "REPLACE_CHEAPER",
                    best.getId(),
                    best.getName(),
                    best.getPrice(),
                    diff,
                    "Preserves requirement '" + item.getRequirementLabel() + "' at lower cost from " + best.getSourcePlatform()
                ));
                accumulatedSavings = accumulatedSavings.add(diff);
                if (accumulatedSavings.compareTo(excess) >= 0) break;
            }
        }

        if (accumulatedSavings.compareTo(excess) < 0) {
            for (MissionItem item : items) {
                if (!item.isSelected() || item.isExcludedOwned()) continue;
                if (!"OPTIONAL".equalsIgnoreCase(item.getPriority())) continue;
                Product currentProd = productMap.get(item.getProductId());
                if (currentProd == null || !isPricedAndValid(currentProd)) continue;
                BigDecimal line = currentProd.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                suggestions.add(new OptimizationSuggestion(
                    item.getId(),
                    currentProd.getName(),
                    currentProd.getPrice(),
                    "DEFER_OPTIONAL",
                    null,
                    null,
                    BigDecimal.ZERO,
                    line,
                    "Optional item — deferring keeps essential items closer to your ₹" + current.totalBudget() + " budget"
                ));
                accumulatedSavings = accumulatedSavings.add(line);
                if (accumulatedSavings.compareTo(excess) >= 0) break;
            }
        }

        BigDecimal projectedPayable = current.finalPayable().subtract(accumulatedSavings).max(BigDecimal.ZERO);
        BigDecimal projectedRemaining = current.totalBudget().subtract(projectedPayable);
        double projectedUtil = current.totalBudget().compareTo(BigDecimal.ZERO) > 0
            ? projectedPayable.multiply(BigDecimal.valueOf(100)).divide(current.totalBudget(), 1, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        BudgetSummary projected = new BudgetSummary(
            current.totalBudget(),
            current.essentialSpending(),
            current.optionalSpending().subtract(accumulatedSavings).max(BigDecimal.ZERO),
            current.productSubtotal().subtract(accumulatedSavings).max(BigDecimal.ZERO),
            projectedPayable,
            current.verifiedDiscount(),
            current.shippingCost(),
            current.applicableTaxes(),
            current.couponDiscount(),
            projectedPayable,
            projectedRemaining,
            projectedUtil,
            current.excludedOwnedSavings(),
            current.unpricedItemCount(),
            projectedPayable.compareTo(current.totalBudget()) > 0,
            projectedPayable.compareTo(current.totalBudget()) <= 0 && current.unpricedItemCount() == 0
        );

        return new OptimizationPlan(current, projected, accumulatedSavings, suggestions);
    }
}
