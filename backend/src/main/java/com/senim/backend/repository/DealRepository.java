package com.senim.backend.repository;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DealRepository extends JpaRepository<Deal, UUID> {

    List<Deal> findAllByAgencyId(UUID agencyId);

    List<Deal> findAllByAgentId(UUID agentId);

    List<Deal> findAllByAgencyIdAndStatus(UUID agencyId, DealStatus status);

    List<Deal> findAllByRiskLevel(RiskLevel riskLevel);

    List<Deal> findAllByStatusIn(List<DealStatus> statuses);

    long countByAgencyIdAndRiskLevel(UUID agencyId, RiskLevel riskLevel);

    /**
     * Updates only riskLevel without touching updatedAt, so the staleness
     * clock (updatedAt) is unaffected by automated risk scans.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Deal d SET d.riskLevel = :riskLevel WHERE d.id = :dealId")
    int updateRiskLevelById(@Param("dealId") UUID dealId, @Param("riskLevel") RiskLevel riskLevel);

    /**
     * Efficient aggregate for commission-at-risk dashboard metric.
     */
    @Query("""
            SELECT COALESCE(SUM(d.commissionUsd), 0)
            FROM Deal d
            WHERE d.agencyId = :agencyId
              AND d.status IN :statuses
              AND d.riskLevel IN :riskLevels
            """)
    BigDecimal sumCommissionAtRisk(
            @Param("agencyId") UUID agencyId,
            @Param("statuses") List<DealStatus> statuses,
            @Param("riskLevels") List<RiskLevel> riskLevels);
}
