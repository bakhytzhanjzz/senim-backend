package com.senim.backend.risk;

import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Elevates a deal to CRITICAL if it has been stuck in IN_PROGRESS for more than 20 days
 * without a status change. Uses statusChangedAt to measure stagnation accurately,
 * unaffected by automated risk-scan writes.
 */
@Component
public class StaleDealRule implements RiskRule {

    private static final int STALE_THRESHOLD_DAYS = 20;

    @Override
    public RiskLevel evaluate(RiskEvaluationContext context) {
        if (context.deal().getStatus() != DealStatus.IN_PROGRESS) {
            return RiskLevel.LOW;
        }

        Instant reference = context.deal().getStatusChangedAt();
        if (reference == null) {
            reference = context.deal().getCreatedAt();
        }
        if (reference == null) {
            return RiskLevel.LOW;
        }

        long daysInProgress = ChronoUnit.DAYS.between(reference, Instant.now());
        return daysInProgress > STALE_THRESHOLD_DAYS ? RiskLevel.CRITICAL : RiskLevel.LOW;
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
