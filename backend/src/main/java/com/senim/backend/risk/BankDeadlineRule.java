package com.senim.backend.risk;

import com.senim.backend.domain.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Evaluates risk based on the number of days remaining until the bank response deadline.
 *
 * CRITICAL : ≤ 2 days
 * HIGH     : ≤ 5 days
 * LOW      : > 5 days or no date set
 */
@Component
public class BankDeadlineRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskEvaluationContext context) {
        LocalDate deadline = context.deal().getBankResponseDeadline();
        if (deadline == null) {
            return RiskLevel.LOW;
        }

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

        if (daysRemaining <= 2) {
            return RiskLevel.CRITICAL;
        }
        if (daysRemaining <= 5) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
