package com.senim.backend.risk;

import com.senim.backend.domain.ChecklistStatus;
import com.senim.backend.domain.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Elevates risk to MEDIUM when any checklist item is in PENDING status,
 * meaning it has been submitted for external confirmation but not yet resolved.
 */
@Component
public class PendingItemsRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskEvaluationContext context) {
        boolean anyPending = context.checklistItems().stream()
                .anyMatch(item -> item.getStatus() == ChecklistStatus.PENDING);

        return anyPending ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
