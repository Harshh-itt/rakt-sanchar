package com.example.blood.repository;

import com.example.blood.model.Notification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notification> store = new ConcurrentHashMap<>();

    @Override
    public synchronized Notification save(Notification notification) {
        if (notification.getDedupKey() != null && !notification.getDedupKey().isBlank()
                && notification.getUserId() != null) {
            Optional<Notification> duplicate = store.values().stream()
                    .filter(n -> notification.getUserId().equals(n.getUserId())
                            && notification.getDedupKey().equals(n.getDedupKey()))
                    .findFirst();
            if (duplicate.isPresent() && !duplicate.get().getId().equals(notification.getId())) {
                return duplicate.get();
            }
        }
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(Instant.now());
            }
        }
        store.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public Optional<Notification> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) return List.of();
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Notification::getId))
                .toList();
    }

    @Override
    public long countUnreadByUserId(String userId) {
        if (userId == null || userId.isBlank()) return 0;
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()) && !n.isRead())
                .count();
    }

    @Override
    public synchronized int markAllReadByUserId(String userId) {
        if (userId == null || userId.isBlank()) return 0;
        int updated = 0;
        for (Notification n : store.values()) {
            if (userId.equals(n.getUserId()) && !n.isRead()) {
                n.setRead(true);
                updated++;
            }
        }
        return updated;
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
