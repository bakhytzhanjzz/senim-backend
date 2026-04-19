package com.senim.backend.service;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.domain.Role;
import com.senim.backend.domain.User;
import com.senim.backend.dto.CreateDealRequest;
import com.senim.backend.dto.DealResponse;
import com.senim.backend.dto.DealSummaryResponse;
import com.senim.backend.exception.BusinessRuleException;
import com.senim.backend.exception.ResourceNotFoundException;
import com.senim.backend.repository.ChecklistItemRepository;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.repository.DocumentRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DealService {

    private static final Map<DealStatus, Set<DealStatus>> LEGAL_TRANSITIONS = new EnumMap<>(DealStatus.class);

    static {
        LEGAL_TRANSITIONS.put(DealStatus.DRAFT,       Set.of(DealStatus.IN_PROGRESS, DealStatus.CANCELLED));
        LEGAL_TRANSITIONS.put(DealStatus.IN_PROGRESS, Set.of(DealStatus.SUBMITTED, DealStatus.CANCELLED));
        LEGAL_TRANSITIONS.put(DealStatus.SUBMITTED,   Set.of(DealStatus.APPROVED, DealStatus.REJECTED));
        LEGAL_TRANSITIONS.put(DealStatus.REJECTED,    Set.of(DealStatus.IN_PROGRESS));
        LEGAL_TRANSITIONS.put(DealStatus.APPROVED,    Set.of());
        LEGAL_TRANSITIONS.put(DealStatus.CANCELLED,   Set.of());
    }

    private static final List<DealStatus> ACTIVE_STATUSES =
            List.of(DealStatus.IN_PROGRESS, DealStatus.SUBMITTED);

    private static final List<RiskLevel> AT_RISK_LEVELS =
            List.of(RiskLevel.HIGH, RiskLevel.CRITICAL);

    private final DealRepository dealRepository;
    private final ChecklistService checklistService;
    private final ChecklistItemRepository checklistItemRepository;
    private final DocumentRepository documentRepository;
    private final RiskEngineService riskEngineService;
    private final CacheManager cacheManager;

    public DealService(
            DealRepository dealRepository,
            ChecklistService checklistService,
            ChecklistItemRepository checklistItemRepository,
            DocumentRepository documentRepository,
            RiskEngineService riskEngineService,
            CacheManager cacheManager) {
        this.dealRepository = dealRepository;
        this.checklistService = checklistService;
        this.checklistItemRepository = checklistItemRepository;
        this.documentRepository = documentRepository;
        this.riskEngineService = riskEngineService;
        this.cacheManager = cacheManager;
    }

    /**
     * Creates a deal in DRAFT status, seeds the default checklist, and calculates
     * the initial risk level based on the provided deadlines.
     */
    @Transactional
    public DealResponse createDeal(CreateDealRequest request, User agent) {
        Deal deal = Deal.builder()
                .agencyId(request.agencyId())
                .agentId(agent.getId())
                .clientName(request.clientName())
                .clientPhone(request.clientPhone())
                .propertyAddress(request.propertyAddress())
                .propertyValueUsd(request.propertyValueUsd())
                .commissionUsd(request.commissionUsd())
                .bankName(request.bankName())
                .mortgageProgramName(request.mortgageProgramName())
                .bookingExpiryDate(request.bookingExpiryDate())
                .bankResponseDeadline(request.bankResponseDeadline())
                .notes(request.notes())
                .status(DealStatus.DRAFT)
                .riskLevel(RiskLevel.LOW)
                .build();

        deal = dealRepository.save(deal);

        checklistService.initializeDefaultChecklist(deal.getId());

        RiskLevel initialRisk = riskEngineService.calculateRiskLevel(deal,
                checklistItemRepository.findAllByDealIdOrderBySortOrder(deal.getId()),
                0L);

        if (initialRisk != RiskLevel.LOW) {
            dealRepository.updateRiskLevelById(deal.getId(), initialRisk);
            deal.setRiskLevel(initialRisk);
        }

        evictDashboardCache(deal.getAgencyId());
        return DealResponse.from(deal);
    }

    /**
     * Returns a deal by ID. Validates the requesting user belongs to the same agency.
     */
    @Transactional(readOnly = true)
    public DealResponse getDealById(UUID dealId, User requestingUser) {
        Deal deal = loadDeal(dealId);
        assertSameAgency(deal, requestingUser);
        return DealResponse.from(deal);
    }

    /**
     * Returns all deals visible to the requesting user:
     * - OWNER: all deals in their agency
     * - AGENT: only their own deals
     */
    @Transactional(readOnly = true)
    public List<DealResponse> getDeals(User requestingUser) {
        List<Deal> deals = requestingUser.getRole() == Role.OWNER
                ? dealRepository.findAllByAgencyId(requestingUser.getAgencyId())
                : dealRepository.findAllByAgentId(requestingUser.getId());

        return deals.stream().map(DealResponse::from).toList();
    }

    /**
     * Transitions a deal to a new status with the following checks:
     * 1. Requesting user must belong to the same agency
     * 2. Transition must be legal per the state machine
     * 3. Hard-gate: SUBMITTED requires a complete checklist
     */
    @Transactional
    public DealResponse updateDealStatus(UUID dealId, DealStatus newStatus, User requestingUser) {
        Deal deal = loadDeal(dealId);
        assertSameAgency(deal, requestingUser);

        Set<DealStatus> allowed = LEGAL_TRANSITIONS.getOrDefault(deal.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessRuleException(
                    String.format("Cannot transition deal from %s to %s", deal.getStatus(), newStatus));
        }

        if (newStatus == DealStatus.SUBMITTED && !checklistService.isChecklistComplete(dealId)) {
            throw new BusinessRuleException("Checklist incomplete — cannot submit");
        }

        deal.setStatus(newStatus);
        deal.setStatusChangedAt(Instant.now());
        deal = dealRepository.save(deal);

        riskEngineService.evaluateAndUpdateDeal(dealId);
        evictDashboardCache(deal.getAgencyId());

        return DealResponse.from(dealRepository.findById(dealId).orElseThrow());
    }

    /**
     * Returns the current risk level for a deal without persisting.
     */
    @Transactional(readOnly = true)
    public RiskLevel calculateRiskLevel(UUID dealId) {
        Deal deal = loadDeal(dealId);
        return riskEngineService.calculateRiskLevel(
                deal,
                checklistItemRepository.findAllByDealIdOrderBySortOrder(dealId),
                documentRepository.countByDealId(dealId));
    }

    /**
     * Sums commission for all active deals in the agency rated HIGH or CRITICAL.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCommissionAtRisk(UUID agencyId) {
        return dealRepository.sumCommissionAtRisk(agencyId, ACTIVE_STATUSES, AT_RISK_LEVELS);
    }

    /**
     * Aggregated dashboard data for an agency owner: deal counts by status and
     * risk level, plus commission at risk. Cached for 5 minutes per agencyId.
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "dashboard", key = "#agencyId.toString()")
    public DealSummaryResponse getDealSummaryForDashboard(UUID agencyId) {
        List<Deal> allDeals = dealRepository.findAllByAgencyId(agencyId);

        Map<DealStatus, Long> byStatus = allDeals.stream()
                .collect(Collectors.groupingBy(Deal::getStatus, () -> new EnumMap<>(DealStatus.class),
                        Collectors.counting()));

        Map<RiskLevel, Long> byRiskLevel = allDeals.stream()
                .collect(Collectors.groupingBy(Deal::getRiskLevel, () -> new EnumMap<>(RiskLevel.class),
                        Collectors.counting()));

        long activeDeals = allDeals.stream()
                .filter(d -> ACTIVE_STATUSES.contains(d.getStatus()))
                .count();

        BigDecimal commissionAtRisk = getTotalCommissionAtRisk(agencyId);

        return new DealSummaryResponse(
                allDeals.size(),
                activeDeals,
                byStatus,
                byRiskLevel,
                commissionAtRisk,
                byRiskLevel.getOrDefault(RiskLevel.CRITICAL, 0L),
                byRiskLevel.getOrDefault(RiskLevel.HIGH, 0L)
        );
    }

    private Deal loadDeal(UUID dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));
    }

    private void assertSameAgency(Deal deal, User user) {
        if (!deal.getAgencyId().equals(user.getAgencyId())) {
            throw new AccessDeniedException("You do not have access to this deal");
        }
    }

    private void evictDashboardCache(UUID agencyId) {
        Cache cache = cacheManager.getCache("dashboard");
        if (cache != null) {
            cache.evict(agencyId.toString());
        }
    }
}
