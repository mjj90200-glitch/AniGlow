package com.aniglow.service;

import com.aniglow.dto.community.CommunityPostDto;
import com.aniglow.entity.Community;
import com.aniglow.entity.CommunityPost;
import com.aniglow.entity.CommunityPostLike;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.CommunityMapper;
import com.aniglow.repository.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityServiceTest {

    private final CommunityRepository communityRepository = mock(CommunityRepository.class);
    private final CommunityPostRepository postRepository = mock(CommunityPostRepository.class);
    private final CommunityPostLikeRepository postLikeRepository = mock(CommunityPostLikeRepository.class);
    private final CommunityReplyRepository replyRepository = mock(CommunityReplyRepository.class);
    private final CommunityReplyLikeRepository replyLikeRepository = mock(CommunityReplyLikeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CommunityMapper mapper = mock(CommunityMapper.class);
    private final CommunityService service = new CommunityService(
            communityRepository,
            postRepository,
            postLikeRepository,
            replyRepository,
            replyLikeRepository,
            userRepository,
            mock(UserProfileService.class),
            mapper
    );

    @Test
    void createsOneRealLikePerUser() {
        User user = User.builder().id(7L).build();
        CommunityPost post = post(3L, 4L);
        CommunityPostDto response = CommunityPostDto.builder().id(3L).likeCount(5L).likedByMe(true).build();
        when(postRepository.findActiveForUpdate(3L)).thenReturn(Optional.of(post));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(postLikeRepository.findByPostIdAndUserId(3L, 7L)).thenReturn(Optional.empty());
        when(postRepository.save(post)).thenReturn(post);
        when(mapper.toPostDto(post, true)).thenReturn(response);

        CommunityPostDto result = service.likePost(3L, 7L);

        assertThat(result.getLikedByMe()).isTrue();
        assertThat(post.getLikeCount()).isEqualTo(5L);
        verify(postLikeRepository).save(any(CommunityPostLike.class));
    }

    @Test
    void secondLikeRequestTogglesExistingLikeOff() {
        User user = User.builder().id(7L).build();
        CommunityPost post = post(3L, 4L);
        CommunityPostLike existing = CommunityPostLike.builder().id(11L).post(post).user(user).build();
        CommunityPostDto response = CommunityPostDto.builder().id(3L).likeCount(3L).likedByMe(false).build();
        when(postRepository.findActiveForUpdate(3L)).thenReturn(Optional.of(post));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(postLikeRepository.findByPostIdAndUserId(3L, 7L)).thenReturn(Optional.of(existing));
        when(postRepository.save(post)).thenReturn(post);
        when(mapper.toPostDto(post, false)).thenReturn(response);

        CommunityPostDto result = service.likePost(3L, 7L);

        assertThat(result.getLikedByMe()).isFalse();
        assertThat(post.getLikeCount()).isEqualTo(3L);
        verify(postLikeRepository).delete(existing);
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void deletedPostCannotBeOpenedOrIncremented() {
        when(postRepository.findActiveForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPost(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(postRepository, never()).save(any());
    }

    private CommunityPost post(Long postId, Long initialLikes) {
        return CommunityPost.builder()
                .id(postId)
                .community(Community.builder().id(1L).heatScore(0L).build())
                .user(User.builder().id(2L).build())
                .likeCount(initialLikes)
                .isDeleted(false)
                .build();
    }
}
