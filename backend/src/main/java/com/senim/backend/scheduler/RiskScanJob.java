package com.senim.backend.scheduler;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.dto.RiskScanResultResponse;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.risk.RiskUpdateResult;
import com.senim.backend.service.NotificationService;
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
    private final NotificationService notificationService;

    public RiskScanJob(
            DealRepository dealRepository,
            RiskEngineService riskEngineService,
            NotificationService notificationService) {
        this.dealRepository = dealRepository;
        this.riskEngineService = riskEngineService;
        this.notificationService = notificationService;
    }

    /**
     * Scans all IN_PROGRESS and SUBMITTED deals every 6 hours.
     * Re-evaluates each deal's risk level, then creates DEAL_CRITICAL notifications
     * whenever a deal's risk escalates to CRITICAL for the first time in this cycle.
     */
    @Scheduled(fixedRateString = "PT6H")
    public RiskScanResultResponse scan() {
        log.info("Risk scan started");

        List<Deal> activeDeals = dealRepository.findAllByStatusIn(
                List.of(DealStatus.IN_PROGRESS, DealStatus.SUBMITTED));

        AtomicInteger updatedCount = new AtomicInteger(0);

        for (Deal deal : activeDeals) {
            try {
                RiskUpdateResult result = riskEngineService.evaluateAndUpdateDeal(deal.getId());

                if (result.changed()) {
                    updatedCount.incrementAndGet();
                }

                if (result.escalatedToCritical()) {
                    notifyDealCritical(deal);
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

    private void notifyDealCritical(Deal deal) {
        try {
            notificationService.notifyDealCritical(deal);
            log.info("CRITICAL notifications sent for deal {} ({})", deal.getId(), deal.getClientName());
        } catch (Exception e) {
            log.error("Failed to send CRITICAL notifications for deal {}: {}", deal.getId(), e.getMessage());
        }
    }
}
