package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.budget.BudgetPlannerService.BudgetSummary;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MissionExplanationService {

    public String explain(ParsedIntent intent, BudgetSummary budget, CompatibilityReport compatibility, List<String> unmatched) {
        StringBuilder sb = new StringBuilder();
        sb.append("MissionCart planned \"").append(intent.title()).append("\" under ")
          .append(intent.purpose()).append(". ");

        if (budget.totalBudget() != null && intent.budget() != null) {
            if (budget.overBudget()) {
                sb.append("Verified payable total (₹").append(budget.finalPayable())
                  .append(" incl. shipping & taxes) exceeds your ₹").append(intent.budget())
                  .append(" budget by ₹").append(budget.finalPayable().subtract(intent.budget()))
                  .append(" — use Optimize Budget to review swaps. ");
            } else if (!budget.affordableConfirmed() && budget.unpricedItemCount() > 0) {
                sb.append("Verified priced items total ₹").append(budget.finalPayable())
                  .append(", while ").append(budget.unpricedItemCount())
                  .append(" item(s) await live retailer price verification before confirming total affordability. ");
            } else {
                sb.append("Verified payable total (₹").append(budget.finalPayable())
                  .append(" incl. ₹").append(budget.shippingCost()).append(" shipping & ₹")
                  .append(budget.applicableTaxes()).append(" tax) fits within your ₹")
                  .append(intent.budget()).append(" budget with ₹")
                  .append(budget.remainingBudget()).append(" remaining. ");
            }
        }
        if (intent.existingPossessions() != null && !intent.existingPossessions().isEmpty()) {
            sb.append("Excluded already-owned items (")
              .append(String.join(", ", intent.existingPossessions()))
              .append(") to prevent duplicate purchases. ");
        }
        if (compatibility != null && !"COMPATIBLE".equals(compatibility.overallStatus())) {
            sb.append("Compatibility engine flagged ").append(compatibility.issues().size())
              .append(" item(s) for review. ");
        }
        if (unmatched != null && !unmatched.isEmpty()) {
            sb.append("Unavailable in verified catalog: ").append(String.join(", ", unmatched)).append(". ");
        }
        if ("DETERMINISTIC_FALLBACK".equals(intent.providerUsed())) {
            sb.append("[Local deterministic rule engine active.]");
        }
        return sb.toString().trim();
    }
}
