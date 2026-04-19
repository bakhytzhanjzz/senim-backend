package com.senim.backend.service;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.Deal;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.exception.ResourceNotFoundException;
import com.senim.backend.repository.ChecklistItemRepository;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.repository.DocumentRepository;
import com.senim.backend.risk.RiskEvaluationContext;
import com.senim.backend.risk.RiskRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RiskEngineService {

    private final List<RiskRule> rules;
    private final DealRepository dealRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final DocumentRepository documentRepository;

    public RiskEngineService(
            List<RiskRule> rules,
            DealRepository dealRepository,
            ChecklistItemRepository checklistItemRepository,
            DocumentRepository documentRepository) {
        this.rules = rules;
        this.dealRepository = dealRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Pure calculation — does not persist. Runs all rules in priority order,
     * short-circuits as soon as CRITICAL is reached, and returns the highest
     * risk level found across all rules.
     */
    public RiskLevel calculateRiskLevel(Deal deal, List<ChecklistItem> items, long documentCount) {
        RiskEvaluationContext ctx = new RiskEvaluationContext(deal, items, documentCount);

        List<RiskRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(RiskRule::getPriority))
                .toList();

        RiskLevel highest = RiskLevel.LOW;
        for (RiskRule rule : sortedRules) {
            RiskLevel level = rule.evaluate(ctx);
            if (level.ordinal() > highest.ordinal()) {
                highest = level;
                log.debug("Rule {} raised risk to {} for deal {}",
                        rule.getClass().getSimpleName(), level, deal.getId());
            }
            if (highest == RiskLevel.CRITICAL) {
                break;
            }
        }
        return highest;
    }

    /**
     * Loads the deal with its current checklist and documents, runs the engine,
     * and persists the new riskLevel only if it changed (without touching updatedAt).
     *
     * @return true if the risk level was changed and persisted
     */
    @Transactional
    public boolean evaluateAndUpdateDeal(UUID dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));

        List<ChecklistItem> items = checklistItemRepository.findAllByDealIdOrderBySortOrder(dealId);
        long docCount = documentRepository.countByDealId(dealId);

        RiskLevel newLevel = calculateRiskLevel(deal, items, docCount);

        if (newLevel != deal.getRiskLevel()) {
            dealRepository.updateRiskLevelById(dealId, newLevel);
            log.info("Risk level updated for deal {}: {} → {}", dealId, deal.getRiskLevel(), newLevel);
            return true;
        }
        return false;
    }
}
