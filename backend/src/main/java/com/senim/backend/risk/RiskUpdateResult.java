package com.senim.backend.risk;

import com.senim.backend.domain.RiskLevel;

/**
 * Result returned by RiskEngineService.evaluateAndUpdateDeal.
 * Carries both previous and new risk levels so callers (e.g. RiskScanJob)
 * can detect meaningful transitions such as escalation to CRITICAL.
 */
public record RiskUpdateResult(
        RiskLevel previousLevel,
        RiskLevel newLevel,
        boolean changed
) {
    public boolean escalatedToCritical() {
        return changed
                && newLevel == RiskLevel.CRITICAL
                && previousLevel != RiskLevel.CRITICAL;
    }
}
