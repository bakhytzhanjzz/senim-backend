package com.senim.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated analytics payload for the Analytics page.
 * All metrics are scoped to the authenticated owner's agency.
 */
public record AnalyticsOverviewResponse(
        List<MonthlyStat> monthly,
        List<BankDistribution> byBank,
        PipelineCounts pipeline,
        BigDecimal totalCommissionClosed,
        BigDecimal averageCommission,
        BigDecimal averageDealValue,
        double conversionRate,
        long dealsLast30Days,
        long dealsLast7Days
) {
    public record MonthlyStat(
            String month,
            long created,
            long approved,
            long rejected,
            BigDecimal approvedCommission
    ) {}

    public record BankDistribution(
            String bank,
            long dealCount,
            BigDecimal totalCommission
    ) {}

    public record PipelineCounts(
            long draft,
            long inProgress,
            long submitted,
            long approved,
            long rejected,
            long cancelled
    ) {}
}
