package com.example.blood.security;

import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.Role;
import com.example.blood.model.User;
import com.example.blood.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Demo-only stub identity provider that reads X-Demo-User-Id and X-Demo-Role headers.
 * WARNING: This stub is NOT security and is active only under the "demo" profile.
 */
@Component
@Profile("demo")
public class DevStubCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER_USER_ID = "X-Demo-User-Id";
    public static final String HEADER_ROLE = "X-Demo-Role";
    public static final String HEADER_NAME = "X-Demo-Name";
    public static final String HEADER_LANGUAGE = "X-Demo-Language";

    private final UserRepository userRepository;

    public DevStubCurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<CurrentUser> getCurrentUserOptional() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return Optional.empty();
        }
        String rawUserId = request.getHeader(HEADER_USER_ID);
        if (rawUserId == null || rawUserId.isBlank()) {
            return Optional.empty();
        }
        String userId = rawUserId.trim();
        String rawRole = request.getHeader(HEADER_ROLE);
        String rawName = request.getHeader(HEADER_NAME);
        String rawLang = request.getHeader(HEADER_LANGUAGE);

        Role headerRole = null;
        if (rawRole != null && !rawRole.isBlank()) {
            try {
                headerRole = Role.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(ErrorCode.IDENTITY_REQUIRED, "Invalid X-Demo-Role header: " + rawRole);
            }
        }

        Optional<User> existingOpt = userRepository.findById(userId);
        User user;
        if (existingOpt.isPresent()) {
            user = existingOpt.get();
            if (headerRole != null && user.getRole() == null) {
                user.setRole(headerRole);
                userRepository.save(user);
            }
        } else {
            if (headerRole == null) {
                throw new ApiException(ErrorCode.IDENTITY_REQUIRED,
                        "X-Demo-Role header is required when X-Demo-User-Id is not pre-seeded");
            }
            String displayName = (rawName != null && !rawName.isBlank())
                    ? rawName.trim()
                    : formatDefaultName(userId, headerRole);
            String lang = normalizeLanguage(rawLang);
            user = new User(userId, displayName, headerRole, false, lang, Instant.now());
            userRepository.save(user);
        }

        Role effectiveRole = headerRole != null ? headerRole : user.getRole();
        if (effectiveRole == null) {
            throw new ApiException(ErrorCode.IDENTITY_REQUIRED, "Role could not be resolved for user " + userId);
        }

        return Optional.of(new CurrentUser(
                user.getId(),
                effectiveRole,
                user.getName(),
                user.getPreferredLanguage(),
                user.isOnboarded()
        ));
    }

    @Override
    public CurrentUser requireCurrentUser() {
        return getCurrentUserOptional().orElseThrow(() ->
                new ApiException(ErrorCode.IDENTITY_REQUIRED,
                        "Demo identity headers (X-Demo-User-Id and X-Demo-Role) are required for this endpoint"));
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private String formatDefaultName(String userId, Role role) {
        return switch (role) {
            case ADMIN -> "Demo Admin (" + userId + ")";
            case HOSPITAL -> "Demo Hospital (" + userId + ")";
            case DONOR -> "Demo Donor (" + userId + ")";
        };
    }

    private String normalizeLanguage(String lang) {
        if (lang != null && lang.trim().equalsIgnoreCase("hi")) {
            return "hi";
        }
        return "en";
    }
}
