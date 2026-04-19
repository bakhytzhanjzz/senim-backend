package com.senim.backend.dto;

import com.senim.backend.domain.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String fullName,
        Role role,
        UUID agencyId
) {}
