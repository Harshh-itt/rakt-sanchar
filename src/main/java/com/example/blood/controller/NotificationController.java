package com.example.blood.controller;

import com.example.blood.dto.NotificationResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.security.CurrentUser;
import com.example.blood.security.CurrentUserProvider;
import com.example.blood.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserProvider currentUserProvider
    ) {
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public PageResponse<NotificationResponse> getMyNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return notificationService.getMyNotifications(currentUser, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        long count = notificationService.getUnreadCount(currentUser);
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable("id") String id) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return notificationService.markAsRead(currentUser, id);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        int updated = notificationService.markAllAsRead(currentUser);
        return Map.of("updatedCount", updated);
    }
}
