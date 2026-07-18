package com.aniglow.service;

import com.aniglow.dto.community.*;
import com.aniglow.entity.Community;
import com.aniglow.entity.CommunityPost;
import com.aniglow.entity.CommunityReply;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.CommunityMapper;
import com.aniglow.repository.CommunityPostRepository;
import com.aniglow.repository.CommunityReplyRepository;
import com.aniglow.repository.CommunityRepository;
import com.aniglow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final CommunityMapper mapper;

    @Cacheable(value = "communityList", key = "'all'")
    @Transactional(readOnly = true)
    public List<CommunityDto> listCommunities() {
        return communityRepository.findAllByOrderByFeaturedDescHeatScoreDescPostCountDescCreatedAtAsc()
                .stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CommunityDto getCommunity(String slug) {
        return mapper.toDto(findCommunity(slug));
    }

    @Transactional(readOnly = true)
    public CommunityPostPageDto listPostsByAnime(Long animeId, int page, int size) {
        PageRequest pageable = page(page, size, 20);
        Page<CommunityPost> result = postRepository
                .findByCommunityRelatedAnimeIdAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
                        animeId, pageable);
        return postPage(result, page, pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public CommunityPostPageDto listPosts(String slug, String sort, int page, int size) {
        PageRequest pageable = page(page, size, 100);
        Page<CommunityPost> result = "hot".equals(sort)
                ? postRepository.findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(slug, pageable)
                : postRepository.findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(slug, pageable);
        return postPage(result, page, pageable.getPageSize());
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public CommunityPostDto createPost(String slug, Long userId, CommunityPostRequest request) {
        Community community = findCommunity(slug);
        User user = findUser(userId);
        userProfileService.syncPublicProfile(user, request.getDisplayName(), request.getAvatarUrl());
        CommunityPost post = CommunityPost.builder()
                .community(community).user(user).title(request.getTitle().trim()).content(request.getContent().trim())
                .coverImage(trimToNull(request.getCoverImage())).images(mapper.serializeImages(request.getImages()))
                .lastRepliedAt(LocalDateTime.now()).likeCount(0L).replyCount(0L).viewCount(0L)
                .pinned(false).featured(false).isDeleted(false).build();
        CommunityPost saved = postRepository.save(post);
        community.setPostCount(postRepository.countByCommunityIdAndIsDeletedFalse(community.getId()));
        community.setHeatScore(orZero(community.getHeatScore()) + 3);
        communityRepository.save(community);
        return mapper.toPostDto(saved);
    }

    @Transactional(readOnly = true)
    public CommunityPostPageDto listMyPosts(Long userId, int page, int size) {
        PageRequest pageable = page(page, size, 100);
        Page<CommunityPost> result = postRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
        return postPage(result, page, pageable.getPageSize());
    }

    @Transactional
    public CommunityPostDto getPost(Long postId) {
        CommunityPost post = findPost(postId);
        post.setViewCount(orZero(post.getViewCount()) + 1);
        return mapper.toPostDto(postRepository.save(post));
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public void deletePost(Long postId, Long userId) {
        CommunityPost post = findPost(postId);
        Community community = post.getCommunity();
        boolean allowed = post.getUser().getId().equals(userId)
                || community.getCreatorId() != null && community.getCreatorId().equals(userId);
        if (!allowed) throw new AccessDeniedException("无权删除此帖子");
        post.setIsDeleted(true);
        postRepository.save(post);
        community.setPostCount(postRepository.countByCommunityIdAndIsDeletedFalse(community.getId()));
        communityRepository.save(community);
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public CommunityPostDto likePost(Long postId) {
        CommunityPost post = findPost(postId);
        post.setLikeCount(orZero(post.getLikeCount()) + 1);
        post.getCommunity().setHeatScore(orZero(post.getCommunity().getHeatScore()) + 1);
        return mapper.toPostDto(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public List<CommunityReplyDto> listReplies(Long postId, int page, int size) {
        return replyRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, page(page, size, 100))
                .getContent().stream().map(mapper::toReplyDto).toList();
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public CommunityReplyDto createReply(Long postId, Long userId, CommunityReplyRequest request) {
        CommunityPost post = findPost(postId);
        User user = findUser(userId);
        userProfileService.syncPublicProfile(user, request.getDisplayName(), request.getAvatarUrl());
        CommunityReply saved = replyRepository.save(CommunityReply.builder()
                .post(post).user(user).content(request.getContent().trim()).likeCount(0L).isDeleted(false).build());
        post.setReplyCount(replyRepository.countByPostIdAndIsDeletedFalse(postId));
        post.setLastRepliedAt(LocalDateTime.now());
        post.getCommunity().setHeatScore(orZero(post.getCommunity().getHeatScore()) + 2);
        postRepository.save(post);
        return mapper.toReplyDto(saved);
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        CommunityReply reply = findReply(replyId);
        CommunityPost post = reply.getPost();
        Community community = post.getCommunity();
        boolean allowed = reply.getUser().getId().equals(userId)
                || post.getUser().getId().equals(userId)
                || community.getCreatorId() != null && community.getCreatorId().equals(userId);
        if (!allowed) throw new AccessDeniedException("无权删除此回复");
        reply.setIsDeleted(true);
        replyRepository.save(reply);
        post.setReplyCount(replyRepository.countByPostIdAndIsDeletedFalse(post.getId()));
        postRepository.save(post);
    }

    @Transactional
    public CommunityReplyDto likeReply(Long replyId) {
        CommunityReply reply = findReply(replyId);
        reply.setLikeCount(orZero(reply.getLikeCount()) + 1);
        return mapper.toReplyDto(replyRepository.save(reply));
    }

    private CommunityPostPageDto postPage(Page<CommunityPost> result, int page, int size) {
        return CommunityPostPageDto.builder().items(result.getContent().stream().map(mapper::toPostDto).toList())
                .total(result.getTotalElements()).page(page).size(size).build();
    }

    private Community findCommunity(String slug) {
        return communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("社区", "slug", slug));
    }

    private CommunityPost findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社区帖子", "id", id));
    }

    private CommunityReply findReply(Long id) {
        return replyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("回复", "id", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", id));
    }

    private PageRequest page(int page, int size, int maxSize) {
        if (page < 0 || size < 1 || size > maxSize) throw new IllegalArgumentException("分页参数无效");
        return PageRequest.of(page, size);
    }

    private Long orZero(Long value) { return value == null ? 0L : value; }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
