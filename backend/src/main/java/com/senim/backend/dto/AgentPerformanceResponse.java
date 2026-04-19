package com.senim.backend.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregated performance metrics for a single agent within an agency.
 * Used by the Agents leaderboard (owner-only).
 */
public record AgentPerformanceResponse(
        UUID id,
        String email,
        String fullName,
        Instant createdAt,
        long totalDeals,
        long activeDeals,
        long approvedDeals,
        long atRiskDeals,
        BigDecimal totalCommission,
        BigDecimal approvedCommission,
        BigDecimal commissionAtRisk
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
