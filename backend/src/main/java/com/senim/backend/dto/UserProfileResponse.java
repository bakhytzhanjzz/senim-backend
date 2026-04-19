package com.senim.backend.dto;

import com.senim.backend.domain.Role;
import com.senim.backend.domain.SubscriptionTier;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        Instant createdAt,
        AgencyInfo agency
) {
    public record AgencyInfo(
            UUID id,
            String name,
            SubscriptionTier subscriptionTier,
            Instant createdAt
    ) {}
}
