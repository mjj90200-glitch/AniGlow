package com.aniglow.service;

import com.aniglow.entity.User;
import com.aniglow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserDisplayNameResolver displayNameResolver;

    public void syncPublicProfile(User user, String displayName, String avatarUrl) {
        boolean changed = false;
        if (displayNameResolver.isSafePublicName(displayName, user.getPhone())
                && !displayName.trim().equals(user.getDisplayName())) {
            user.setDisplayName(displayName.trim());
            changed = true;
        }
        if (displayNameResolver.hasText(avatarUrl) && !avatarUrl.trim().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl.trim());
            changed = true;
        }
        if (changed) userRepository.save(user);
    }
}
