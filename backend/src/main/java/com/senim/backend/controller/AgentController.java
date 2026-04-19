package com.senim.backend.controller;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.domain.Role;
import com.senim.backend.domain.User;
import com.senim.backend.dto.AgentPerformanceResponse;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private static final List<DealStatus> ACTIVE_STATUSES =
            List.of(DealStatus.IN_PROGRESS, DealStatus.SUBMITTED);
    private static final List<RiskLevel> AT_RISK_LEVELS =
            List.of(RiskLevel.HIGH, RiskLevel.CRITICAL);

    private final UserRepository userRepository;
    private final DealRepository dealRepository;

    public AgentController(UserRepository userRepository, DealRepository dealRepository) {
        this.userRepository = userRepository;
        this.dealRepository = dealRepository;
    }

    /**
     * Returns all agents in the authenticated owner's agency along with
     * per-agent aggregate performance metrics. OWNER only.
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AgentPerformanceResponse>> getAgents(
            @AuthenticationPrincipal User currentUser) {

        UUID agencyId = currentUser.getAgencyId();
        List<User> agents = userRepository.findByAgencyIdAndRole(agencyId, Role.AGENT);
        List<Deal> allDeals = dealRepository.findAllByAgencyId(agencyId);

        Map<UUID, List<Deal>> byAgent = allDeals.stream()
                .collect(Collectors.groupingBy(Deal::getAgentId));

        List<AgentPerformanceResponse> result = agents.stream()
                .map(agent -> buildMetrics(agent, byAgent.getOrDefault(agent.getId(), List.of())))
                .toList();

        return ResponseEntity.ok(result);
    }

    private AgentPerformanceResponse buildMetrics(User agent, List<Deal> deals) {
        long total = deals.size();
        long active = deals.stream().filter(d -> ACTIVE_STATUSES.contains(d.getStatus())).count();
        long approved = deals.stream().filter(d -> d.getStatus() == DealStatus.APPROVED).count();
        long atRisk = deals.stream()
                .filter(d -> ACTIVE_STATUSES.contains(d.getStatus()) && AT_RISK_LEVELS.contains(d.getRiskLevel()))
                .count();

        BigDecimal totalCommission = sumCommission(deals);
        BigDecimal approvedCommission = sumCommission(deals.stream()
                .filter(d -> d.getStatus() == DealStatus.APPROVED)
                .toList());
        BigDecimal commissionAtRisk = sumCommission(deals.stream()
                .filter(d -> ACTIVE_STATUSES.contains(d.getStatus())
                        && AT_RISK_LEVELS.contains(d.getRiskLevel()))
                .toList());

        return new AgentPerformanceResponse(
                agent.getId(),
                agent.getEmail(),
                agent.getFullName(),
                agent.getCreatedAt(),
                total,
                active,
                approved,
                atRisk,
                totalCommission,
                approvedCommission,
                commissionAtRisk
        );
    }

    private BigDecimal sumCommission(List<Deal> deals) {
        return deals.stream()
                .map(Deal::getCommissionUsd)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
