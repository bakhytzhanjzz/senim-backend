package com.senim.backend.risk;

import com.senim.backend.domain.RiskLevel;

/**
 * A single risk evaluation rule. All implementations are Spring @Components
 * and are collected by RiskEngineService for evaluation.
 *
 * <p>getPriority controls evaluation order — lower value = evaluated first.
 * The engine short-circuits as soon as CRITICAL is reached.
 */
public interface RiskRule {

    RiskLevel evaluate(RiskEvaluationContext context);

    /**
     * Lower number = higher priority. Evaluated before higher-numbered rules.
     * Rules that can produce CRITICAL should have lower numbers for early exit.
     */
    int getPriority();
}
