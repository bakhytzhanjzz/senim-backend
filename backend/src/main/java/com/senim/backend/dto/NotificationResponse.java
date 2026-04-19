package com.senim.backend.dto;

import com.senim.backend.domain.Notification;
import com.senim.backend.domain.NotificationType;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        UUID dealId,
        NotificationType type,
        String message,
        boolean isRead,
        Instant createdAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getDealId(),
                n.getType(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
