package com.example.blood.security;

import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.Role;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AccessPolicy {

    public CurrentUser requireAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.userId().isBlank()) {
            throw new ApiException(ErrorCode.IDENTITY_REQUIRED, "Authentication identity is required");
        }
        if (currentUser.role() == null) {
            throw new ApiException(ErrorCode.IDENTITY_REQUIRED, "User role is required");
        }
        return currentUser;
    }

    public CurrentUser requireRole(CurrentUser currentUser, Role... allowedRoles) {
        requireAuthenticated(currentUser);
        if (allowedRoles == null || allowedRoles.length == 0) {
            return currentUser;
        }
        for (Role allowed : allowedRoles) {
            if (currentUser.role() == allowed) {
                return currentUser;
            }
        }
        throw new ApiException(ErrorCode.FORBIDDEN,
                "Role " + currentUser.role() + " is not permitted; required one of " + Arrays.toString(allowedRoles));
    }

    public void requireOwner(CurrentUser currentUser, String resourceOwnerUserId) {
        requireAuthenticated(currentUser);
        if (resourceOwnerUserId == null || !resourceOwnerUserId.equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You do not own this resource");
        }
    }

    public void requireOwnerOrAdmin(CurrentUser currentUser, String resourceOwnerUserId) {
        requireAuthenticated(currentUser);
        if (currentUser.role() == Role.ADMIN) {
            return;
        }
        if (resourceOwnerUserId == null || !resourceOwnerUserId.equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You do not own this resource");
        }
    }
}
