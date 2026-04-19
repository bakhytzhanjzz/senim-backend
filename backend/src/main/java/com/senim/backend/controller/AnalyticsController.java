package com.senim.backend.controller;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.User;
import com.senim.backend.dto.AnalyticsOverviewResponse;
import com.senim.backend.dto.AnalyticsOverviewResponse.BankDistribution;
import com.senim.backend.dto.AnalyticsOverviewResponse.MonthlyStat;
import com.senim.backend.dto.AnalyticsOverviewResponse.PipelineCounts;
import com.senim.backend.repository.DealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final DealRepository dealRepository;

    public AnalyticsController(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    /**
     * Agency-wide analytics: last 6 months time series, bank distribution,
     * pipeline counts, and several headline KPIs. OWNER only.
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @AuthenticationPrincipal User owner) {

        List<Deal> deals = dealRepository.findAllByAgencyId(owner.getAgencyId());

        return ResponseEntity.ok(new AnalyticsOverviewResponse(
                buildMonthlyStats(deals),
                buildBankDistribution(deals),
                buildPipeline(deals),
                totalClosedCommission(deals),
                averageCommission(deals),
                averageDealValue(deals),
                conversionRate(deals),
                countInWindow(deals, 30),
                countInWindow(deals, 7)
        ));
    }

    private List<MonthlyStat> buildMonthlyStats(List<Deal> deals) {
        YearMonth current = YearMonth.now(ZONE);
        Map<YearMonth, long[]> counts = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> approvedCommissions = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            counts.put(ym, new long[]{0, 0, 0});
            approvedCommissions.put(ym, BigDecimal.ZERO);
        }

        for (Deal d : deals) {
            YearMonth dealMonth = YearMonth.from(d.getCreatedAt().atZone(ZONE).toLocalDate());
            long[] slot = counts.get(dealMonth);
            if (slot != null) {
                slot[0]++;
                if (d.getStatus() == DealStatus.APPROVED) {
                    slot[1]++;
                    BigDecimal prev = approvedCommissions.getOrDefault(dealMonth, BigDecimal.ZERO);
                    approvedCommissions.put(dealMonth,
                            prev.add(d.getCommissionUsd() != null ? d.getCommissionUsd() : BigDecimal.ZERO));
                }
                if (d.getStatus() == DealStatus.REJECTED) {
                    slot[2]++;
                }
            }
        }

        List<MonthlyStat> result = new ArrayList<>();
        counts.forEach((ym, arr) -> result.add(new MonthlyStat(
                ym.toString(),
                arr[0],
                arr[1],
                arr[2],
                approvedCommissions.getOrDefault(ym, BigDecimal.ZERO)
        )));
        return result;
    }

    private List<BankDistribution> buildBankDistribution(List<Deal> deals) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> sums = new LinkedHashMap<>();

        for (Deal d : deals) {
            String bank = d.getBankName() != null ? d.getBankName() : "Не указан";
            counts.computeIfAbsent(bank, k -> new long[]{0})[0]++;
            BigDecimal prev = sums.getOrDefault(bank, BigDecimal.ZERO);
            sums.put(bank, prev.add(d.getCommissionUsd() != null ? d.getCommissionUsd() : BigDecimal.ZERO));
        }

        List<BankDistribution> list = new ArrayList<>();
        counts.forEach((bank, arr) -> list.add(new BankDistribution(bank, arr[0], sums.get(bank))));
        list.sort(Comparator.comparingLong(BankDistribution::dealCount).reversed());
        return list;
    }

    private PipelineCounts buildPipeline(List<Deal> deals) {
        long draft = 0, inProgress = 0, submitted = 0, approved = 0, rejected = 0, cancelled = 0;
        for (Deal d : deals) {
            switch (d.getStatus()) {
                case DRAFT -> draft++;
                case IN_PROGRESS -> inProgress++;
                case SUBMITTED -> submitted++;
                case APPROVED -> approved++;
                case REJECTED -> rejected++;
                case CANCELLED -> cancelled++;
            }
        }
        return new PipelineCounts(draft, inProgress, submitted, approved, rejected, cancelled);
    }

    private BigDecimal totalClosedCommission(List<Deal> deals) {
        return deals.stream()
                .filter(d -> d.getStatus() == DealStatus.APPROVED)
                .map(Deal::getCommissionUsd)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averageCommission(List<Deal> deals) {
        List<BigDecimal> commissions = deals.stream()
                .map(Deal::getCommissionUsd)
                .filter(c -> c != null && c.compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (commissions.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = commissions.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(commissions.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageDealValue(List<Deal> deals) {
        List<BigDecimal> values = deals.stream()
                .map(Deal::getPropertyValueUsd)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private double conversionRate(List<Deal> deals) {
        long approved = deals.stream().filter(d -> d.getStatus() == DealStatus.APPROVED).count();
        long closed = deals.stream()
                .filter(d -> d.getStatus() == DealStatus.APPROVED
                          || d.getStatus() == DealStatus.REJECTED
                          || d.getStatus() == DealStatus.CANCELLED)
                .count();
        if (closed == 0) return 0.0;
        return Math.round(((double) approved / closed) * 1000.0) / 10.0;
    }

    private long countInWindow(List<Deal> deals, int days) {
        Instant threshold = Instant.now().minus(days, ChronoUnit.DAYS);
        return deals.stream()
                .filter(d -> d.getCreatedAt().isAfter(threshold))
                .count();
    }
}
