package com.senim.backend.controller;

import com.senim.backend.domain.User;
import com.senim.backend.dto.NotificationResponse;
import com.senim.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns all unread notifications for the authenticated user, newest first.
     * Result is cached per user for 60 seconds.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.getUnreadForUser(currentUser.getId()));
    }

    /**
     * Marks a single notification as read. Returns 403 if the notification
     * does not belong to the authenticated user.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks all of the authenticated user's unread notifications as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
