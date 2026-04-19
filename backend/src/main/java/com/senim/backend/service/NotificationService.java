package com.senim.backend.service;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.Notification;
import com.senim.backend.domain.NotificationType;
import com.senim.backend.domain.Role;
import com.senim.backend.dto.NotificationResponse;
import com.senim.backend.exception.ResourceNotFoundException;
import com.senim.backend.repository.NotificationRepository;
import com.senim.backend.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a notification and evicts the recipient's unread cache.
     */
    @Transactional
    @CacheEvict(value = "notifications", key = "#userId.toString()")
    public NotificationResponse createNotification(
            UUID userId, UUID dealId, NotificationType type, String message) {
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .userId(userId)
                        .dealId(dealId)
                        .type(type)
                        .message(message)
                        .build()
        );
        return NotificationResponse.from(notification);
    }

    /**
     * Creates DEAL_CRITICAL notifications for the deal's agent and all OWNER users
     * in the same agency. Called by RiskScanJob when a deal escalates to CRITICAL.
     */
    @Transactional
    public void notifyDealCritical(Deal deal) {
        String message = String.format(
                "Deal '%s' has escalated to CRITICAL risk — immediate attention required",
                deal.getClientName());

        createNotification(deal.getAgentId(), deal.getId(), NotificationType.DEAL_CRITICAL, message);

        userRepository.findByAgencyIdAndRole(deal.getAgencyId(), Role.OWNER)
                .forEach(owner -> createNotification(
                        owner.getId(), deal.getId(), NotificationType.DEAL_CRITICAL, message));
    }

    /**
     * Returns unread notifications for a user, newest first.
     * Cached for 60 seconds to reduce DB load on frequent polling.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications", key = "#userId.toString()")
    public List<NotificationResponse> getUnreadForUser(UUID userId) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * Marks a single notification as read. Validates ownership before mutating.
     */
    @Transactional
    @CacheEvict(value = "notifications", key = "#userId.toString()")
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all unread notifications for a user as read in a single query.
     */
    @Transactional
    @CacheEvict(value = "notifications", key = "#userId.toString()")
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
