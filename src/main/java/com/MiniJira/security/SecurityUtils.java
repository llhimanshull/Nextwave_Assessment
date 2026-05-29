package com.minijira.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    public static UUID getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static UUID getCurrentOrganizationId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getOrganizationId() : null;
    }
}
