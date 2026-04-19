package com.senim.backend.scheduler;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.dto.RiskScanResultResponse;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.service.RiskEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RiskScanJob {

    private final DealRepository dealRepository;
    private final RiskEngineService riskEngineService;

    public RiskScanJob(DealRepository dealRepository, RiskEngineService riskEngineService) {
        this.dealRepository = dealRepository;
        this.riskEngineService = riskEngineService;
    }

    /**
     * Scans all active deals every 6 hours and recalculates their risk levels.
     * Uses fixedRate so the interval is measured from job completion, avoiding
     * pile-ups if the scan takes longer than expected.
     */
    @Scheduled(fixedRateString = "PT6H")
    public RiskScanResultResponse scan() {
        log.info("Risk scan started");

        List<Deal> activeDeals = dealRepository.findAllByStatusIn(
                List.of(DealStatus.IN_PROGRESS, DealStatus.SUBMITTED));

        AtomicInteger updatedCount = new AtomicInteger(0);

        for (Deal deal : activeDeals) {
            try {
                boolean updated = riskEngineService.evaluateAndUpdateDeal(deal.getId());
                if (updated) {
                    updatedCount.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("Risk evaluation failed for deal {}: {}", deal.getId(), e.getMessage());
            }
        }

        long criticalCount = dealRepository.findAllByRiskLevel(RiskLevel.CRITICAL).stream()
                .filter(d -> d.getStatus() == DealStatus.IN_PROGRESS
                        || d.getStatus() == DealStatus.SUBMITTED)
                .count();

        RiskScanResultResponse result = new RiskScanResultResponse(
                activeDeals.size(),
                updatedCount.get(),
                criticalCount,
                Instant.now()
        );

        log.info("Risk scan complete: {} deals scanned, {} updated, {} critical",
                result.totalScanned(), result.updated(), result.criticalCount());

        return result;
    }
}
