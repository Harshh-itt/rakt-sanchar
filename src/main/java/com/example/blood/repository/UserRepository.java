package com.example.blood.repository;

import com.example.blood.model.Role;
import com.example.blood.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    List<User> findAll();

    List<User> findByRole(Role role);

    void deleteAll();
}
