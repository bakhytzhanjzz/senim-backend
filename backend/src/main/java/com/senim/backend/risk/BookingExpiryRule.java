package com.senim.backend.risk;

import com.senim.backend.domain.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Evaluates risk based on the number of days remaining until booking expiry.
 *
 * CRITICAL : ≤ 2 days
 * HIGH     : ≤ 5 days
 * MEDIUM   : ≤ 10 days
 * LOW      : > 10 days or no date set
 */
@Component
public class BookingExpiryRule implements RiskRule {

    @Override
    public RiskLevel evaluate(RiskEvaluationContext context) {
        LocalDate expiry = context.deal().getBookingExpiryDate();
        if (expiry == null) {
            return RiskLevel.LOW;
        }

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiry);

        if (daysRemaining <= 2) {
            return RiskLevel.CRITICAL;
        }
        if (daysRemaining <= 5) {
            return RiskLevel.HIGH;
        }
        if (daysRemaining <= 10) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
