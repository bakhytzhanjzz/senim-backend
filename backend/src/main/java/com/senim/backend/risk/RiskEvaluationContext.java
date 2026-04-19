package com.senim.backend.risk;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.Deal;

import java.util.List;

/**
 * Immutable context passed to every RiskRule during evaluation.
 * Bundles all data needed so each rule stays stateless.
 */
public record RiskEvaluationContext(
        Deal deal,
        List<ChecklistItem> checklistItems,
        long documentCount
) {}
