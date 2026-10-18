package com.example.blood.repository;

import com.example.blood.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    /**
     * Saves a notification. If {@code dedupKey} is non-null and a notification with the same
     * {@code userId + dedupKey} already exists, returns the existing notification without creating a duplicate.
     */
    Notification save(Notification notification);

    Optional<Notification> findById(String id);

    List<Notification> findByUserId(String userId);

    long countUnreadByUserId(String userId);

    int markAllReadByUserId(String userId);

    void deleteAll();
}
