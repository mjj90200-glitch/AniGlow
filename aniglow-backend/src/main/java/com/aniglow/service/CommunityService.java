package com.aniglow.service;

import com.aniglow.dto.community.*;
import com.aniglow.entity.Community;
import com.aniglow.entity.CommunityPost;
import com.aniglow.entity.CommunityPostLike;
import com.aniglow.entity.CommunityReply;
import com.aniglow.entity.CommunityReplyLike;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.CommunityMapper;
import com.aniglow.repository.CommunityPostRepository;
import com.aniglow.repository.CommunityPostLikeRepository;
import com.aniglow.repository.CommunityReplyRepository;
import com.aniglow.repository.CommunityReplyLikeRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String ALL_COMMUNITIES_SLUG = "all";
    private static final List<String> FIXED_COMMUNITY_SLUGS = List.of(
            "anime", "manga", "game", "vibe-coding", "chat", "partner"
    );

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityReplyRepository replyRepository;
    private final CommunityReplyLikeRepository replyLikeRepository;
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final CommunityMapper mapper;

    @Cacheable(value = "communityList", key = "'all'")
    @Transactional(readOnly = true)
    public List<CommunityDto> listCommunities() {
        List<CommunityDto> fixed = communityRepository.findAllByOrderByFeaturedDescHeatScoreDescPostCountDescCreatedAtAsc()
                .stream()
                .filter(community -> FIXED_COMMUNITY_SLUGS.contains(community.getSlug()))
                .map(mapper::toDto)
                .toList();
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(allCommunitiesDto()), fixed.stream()).toList();
    }

    @Transactional(readOnly = true)
    public CommunityDto getCommunity(String slug) {
        if (ALL_COMMUNITIES_SLUG.equals(slug)) return allCommunitiesDto();
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
        Page<CommunityPost> result;
        if (ALL_COMMUNITIES_SLUG.equals(slug)) {
            result = "hot".equals(sort)
                    ? postRepository.findByCommunitySlugInAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(
                            FIXED_COMMUNITY_SLUGS, pageable)
                    : postRepository.findByCommunitySlugInAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
                            FIXED_COMMUNITY_SLUGS, pageable);
        } else {
            findCommunity(slug);
            result = "hot".equals(sort)
                    ? postRepository.findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(slug, pageable)
                    : postRepository.findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(slug, pageable);
        }
        return postPage(result, page, pageable.getPageSize());
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public CommunityPostDto createPost(String slug, Long userId, CommunityPostRequest request) {
        if (ALL_COMMUNITIES_SLUG.equals(slug)) {
            throw new IllegalArgumentException("请先选择一个具体社区再发布帖子");
        }
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
    public CommunityPostDto getPost(Long postId, Long viewerUserId) {
        CommunityPost post = findPostForUpdate(postId);
        post.setViewCount(orZero(post.getViewCount()) + 1);
        CommunityPost saved = postRepository.save(post);
        boolean liked = viewerUserId != null && postLikeRepository.existsByPostIdAndUserId(postId, viewerUserId);
        return mapper.toPostDto(saved, liked);
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public void deletePost(Long postId, Long userId) {
        CommunityPost post = findPostForUpdate(postId);
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
    public CommunityPostDto likePost(Long postId, Long userId) {
        CommunityPost post = findPostForUpdate(postId);
        User user = findUser(userId);
        var existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0L, orZero(post.getLikeCount()) - 1));
            liked = false;
        } else {
            postLikeRepository.save(CommunityPostLike.builder().post(post).user(user).build());
            post.setLikeCount(orZero(post.getLikeCount()) + 1);
            post.getCommunity().setHeatScore(orZero(post.getCommunity().getHeatScore()) + 1);
            liked = true;
        }
        return mapper.toPostDto(postRepository.save(post), liked);
    }

    @Transactional(readOnly = true)
    public List<CommunityReplyDto> listReplies(Long postId, Long viewerUserId, int page, int size) {
        findPost(postId);
        List<CommunityReply> replies = replyRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, page(page, size, 100))
                .getContent();
        Set<Long> likedIds = viewerUserId == null || replies.isEmpty()
                ? Set.of()
                : new HashSet<>(replyLikeRepository.findLikedReplyIds(
                        viewerUserId, replies.stream().map(CommunityReply::getId).toList()));
        return replies.stream().map(reply -> mapper.toReplyDto(reply, likedIds.contains(reply.getId()))).toList();
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
        CommunityReply reply = findReplyForUpdate(replyId);
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
    public CommunityReplyDto likeReply(Long replyId, Long userId) {
        CommunityReply reply = findReplyForUpdate(replyId);
        User user = findUser(userId);
        var existing = replyLikeRepository.findByReplyIdAndUserId(replyId, userId);
        boolean liked;
        if (existing.isPresent()) {
            replyLikeRepository.delete(existing.get());
            reply.setLikeCount(Math.max(0L, orZero(reply.getLikeCount()) - 1));
            liked = false;
        } else {
            replyLikeRepository.save(CommunityReplyLike.builder().reply(reply).user(user).build());
            reply.setLikeCount(orZero(reply.getLikeCount()) + 1);
            liked = true;
        }
        return mapper.toReplyDto(replyRepository.save(reply), liked);
    }

    @Transactional
    public CommunityPostDto overridePostStats(Long postId, CommunityStatsOverrideRequest request) {
        CommunityPost post = findPostForUpdate(postId);
        if (request.getLikeCount() == null && request.getViewCount() == null) {
            throw new IllegalArgumentException("至少提供点赞数或浏览数");
        }
        if (request.getLikeCount() != null) post.setLikeCount(request.getLikeCount());
        if (request.getViewCount() != null) post.setViewCount(request.getViewCount());
        return mapper.toPostDto(postRepository.save(post));
    }

    @Transactional
    public CommunityReplyDto overrideReplyStats(Long replyId, CommunityStatsOverrideRequest request) {
        CommunityReply reply = findReplyForUpdate(replyId);
        if (request.getLikeCount() == null) throw new IllegalArgumentException("请提供点赞数");
        reply.setLikeCount(request.getLikeCount());
        return mapper.toReplyDto(replyRepository.save(reply));
    }

    @CacheEvict(value = "communityList", allEntries = true)
    @Transactional
    public CommunityDto overrideCommunityStats(String slug, CommunityStatsOverrideRequest request) {
        Community community = findCommunity(slug);
        if (request.getMemberCount() == null && request.getHeatScore() == null) {
            throw new IllegalArgumentException("至少提供成员数或热度");
        }
        if (request.getMemberCount() != null) community.setMemberCount(request.getMemberCount());
        if (request.getHeatScore() != null) community.setHeatScore(request.getHeatScore());
        return mapper.toDto(communityRepository.save(community));
    }

    private CommunityPostPageDto postPage(Page<CommunityPost> result, int page, int size) {
        return CommunityPostPageDto.builder().items(result.getContent().stream().map(mapper::toPostDto).toList())
                .total(result.getTotalElements()).page(page).size(size).build();
    }

    private Community findCommunity(String slug) {
        if (!FIXED_COMMUNITY_SLUGS.contains(slug)) {
            throw new ResourceNotFoundException("社区", "slug", slug);
        }
        return communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("社区", "slug", slug));
    }

    private CommunityPost findPost(Long id) {
        return postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("社区帖子", "id", id));
    }

    private CommunityPost findPostForUpdate(Long id) {
        return postRepository.findActiveForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("社区帖子", "id", id));
    }

    private CommunityReply findReplyForUpdate(Long id) {
        return replyRepository.findActiveForUpdate(id)
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

    private CommunityDto allCommunitiesDto() {
        return CommunityDto.builder()
                .id(0L)
                .slug(ALL_COMMUNITIES_SLUG)
                .name("全部帖子")
                .description("浏览六个固定社区中的全部帖子，发现此刻大家正在讨论的内容。")
                .coverImage("/community-covers/chat.jpg")
                .category("广场")
                .tags(List.of("全部", "最新", "热门"))
                .postCount(0L)
                .memberCount(0L)
                .heatScore(0L)
                .featured(true)
                .build();
    }
}
