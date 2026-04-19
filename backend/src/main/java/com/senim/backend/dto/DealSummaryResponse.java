package com.senim.backend.dto;

import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;

import java.math.BigDecimal;
import java.util.Map;

public record DealSummaryResponse(
        long totalDeals,
        long totalActiveDeals,
        Map<DealStatus, Long> byStatus,
        Map<RiskLevel, Long> byRiskLevel,
        BigDecimal totalCommissionAtRisk,
        long criticalDeals,
        long highRiskDeals
) {}
