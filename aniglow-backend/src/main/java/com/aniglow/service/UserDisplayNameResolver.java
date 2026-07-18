package com.aniglow.service;

import com.aniglow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserDisplayNameResolver {

    public String resolvePublic(User user) {
        if (hasText(user.getDisplayName())) return user.getDisplayName();
        if (isNonPhoneUsername(user.getUsername(), user.getPhone())) return user.getUsername();
        return "番舍同好";
    }

    public String resolveProfile(User user) {
        String value = resolvePublic(user);
        return "番舍同好".equals(value) ? "" : value;
    }

    public boolean isSafePublicName(String value, String phone) {
        if (!hasText(value)) return false;
        String trimmed = value.trim();
        return !"番舍同好".equals(trimmed)
                && !trimmed.equals(phone)
                && !trimmed.matches("^1\\d{10}$");
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isNonPhoneUsername(String username, String phone) {
        return hasText(username)
                && !username.equals(phone)
                && !username.matches("^1\\d{10}$");
    }
}
