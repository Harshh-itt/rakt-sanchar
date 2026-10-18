package com.example.blood.security;

import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback CurrentUserProvider active when the "demo" profile is NOT enabled.
 * Rejects protected calls rather than trusting demo headers outside demo mode.
 */
@Component
@Profile("!demo")
public class DisabledCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> getCurrentUserOptional() {
        return Optional.empty();
    }

    @Override
    public CurrentUser requireCurrentUser() {
        throw new ApiException(ErrorCode.IDENTITY_REQUIRED,
                "Authentication is not configured outside the 'demo' profile");
    }
}
