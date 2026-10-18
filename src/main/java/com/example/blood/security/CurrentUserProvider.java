package com.example.blood.security;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<CurrentUser> getCurrentUserOptional();

    CurrentUser requireCurrentUser();
}
