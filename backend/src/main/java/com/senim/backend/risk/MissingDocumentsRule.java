package com.senim.backend.risk;

import com.senim.backend.domain.ChecklistStatus;
import com.senim.backend.domain.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Evaluates risk based on missing checklist items and total document count.
 *
 * CRITICAL : more than 2 items with status MISSING
 * HIGH     : any item with status MISSING, OR zero documents uploaded to the deal
 * LOW      : all items accounted for and at least one document present
 */
@Component
public class MissingDocumentsRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskEvaluationContext context) {
        long missingCount = context.checklistItems().stream()
                .filter(item -> item.getStatus() == ChecklistStatus.MISSING)
                .count();

        if (missingCount > 2) {
            return RiskLevel.CRITICAL;
        }
        if (missingCount > 0) {
            return RiskLevel.HIGH;
        }
        if (context.documentCount() == 0) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
