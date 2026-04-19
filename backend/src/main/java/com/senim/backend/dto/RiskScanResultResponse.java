package com.senim.backend.dto;

import java.time.Instant;

public record RiskScanResultResponse(
        int totalScanned,
        int updated,
        long criticalCount,
        Instant scannedAt
) {}
