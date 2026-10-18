package com.example.blood.repository;

import com.example.blood.model.Role;
import com.example.blood.model.User;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentMap<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId("usr-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        if (user.getPreferredLanguage() == null || user.getPreferredLanguage().isBlank()) {
            user.setPreferredLanguage("en");
        }
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<User> findByRole(Role role) {
        return store.values().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
