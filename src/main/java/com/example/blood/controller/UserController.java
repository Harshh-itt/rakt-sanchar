package com.example.blood.controller;

import com.example.blood.dto.LanguageUpdateRequest;
import com.example.blood.dto.UserOnboardingRequest;
import com.example.blood.dto.UserResponse;
import com.example.blood.security.CurrentUser;
import com.example.blood.security.CurrentUserProvider;
import com.example.blood.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public java.util.List<UserResponse> listUsers() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return userService.listAllUsers(currentUser);
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return userService.getCurrentUserProfile(currentUser);
    }

    @PostMapping("/me/onboarding")
    public UserResponse completeOnboarding(@Valid @RequestBody UserOnboardingRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return userService.completeOnboarding(currentUser, request);
    }

    @PatchMapping("/me/language")
    public UserResponse updateLanguage(@Valid @RequestBody LanguageUpdateRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return userService.updateLanguage(currentUser, request);
    }
}
