package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.community.*;
import com.aniglow.entity.Community;
import com.aniglow.entity.CommunityPost;
import com.aniglow.entity.CommunityReply;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.CommunityPostRepository;
import com.aniglow.repository.CommunityReplyRepository;
import com.aniglow.repository.CommunityRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.aniglow.service.ImageStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/communities")
@RequiredArgsConstructor
@Tag(name = "社区", description = "盒子化社区、帖子和回复接口")
public class CommunityController {

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityReplyRepository communityReplyRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    @PostMapping("/upload/images")
    @Operation(summary = "上传帖子图片", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = imageStorageService.storeImages(files);
            return ResponseEntity.ok(ApiResponse.success("上传成功", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "社区盒子列表")
    public ResponseEntity<ApiResponse<List<CommunityDto>>> listCommunities() {
        List<CommunityDto> communities = communityRepository
                .findAllByOrderByFeaturedDescHeatScoreDescPostCountDescCreatedAtAsc()
                .stream()
                .map(this::convertCommunity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(communities));
    }

    @GetMapping("/posts/by-anime/{animeId}")
    @Operation(summary = "番剧相关社区帖子", description = "返回与指定番剧关联社区中的最近帖子")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listPostsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 20));
        Page<CommunityPost> posts = communityPostRepository
                .findByCommunityRelatedAnimeIdAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
                        animeId,
                        pageable);

        CommunityPostPageDto pageDto = CommunityPostPageDto.builder()
                .items(posts.getContent().stream().map(this::convertPost).toList())
                .total(posts.getTotalElements())
                .page(page)
                .size(pageable.getPageSize())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageDto));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "社区详情")
    public ResponseEntity<ApiResponse<CommunityDto>> getCommunity(@PathVariable String slug) {
        Community community = findCommunity(slug);
        return ResponseEntity.ok(ApiResponse.success(convertCommunity(community)));
    }

    @GetMapping("/{slug}/posts")
    @Operation(summary = "社区帖子列表")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CommunityPost> posts = "hot".equals(sort)
                ? communityPostRepository.findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(slug, pageable)
                : communityPostRepository.findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(slug, pageable);

        CommunityPostPageDto pageDto = CommunityPostPageDto.builder()
                .items(posts.getContent().stream().map(this::convertPost).toList())
                .total(posts.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageDto));
    }

    @PostMapping("/{slug}/posts")
    @Operation(summary = "发布社区帖子", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<CommunityPostDto>> createPost(
            @PathVariable String slug,
            @Valid @RequestBody CommunityPostRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Community community = findCommunity(slug);
        User user = findUser(userDetails.getId());
        syncPublicProfile(user, request.getDisplayName(), request.getAvatarUrl());

        CommunityPost post = CommunityPost.builder()
                .community(community)
                .user(user)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .coverImage(trimToNull(request.getCoverImage()))
                .images(serializeImages(request.getImages()))
                .lastRepliedAt(LocalDateTime.now())
                .likeCount(0L)
                .replyCount(0L)
                .viewCount(0L)
                .pinned(false)
                .featured(false)
                .isDeleted(false)
                .build();

        CommunityPost saved = communityPostRepository.save(post);
        community.setPostCount(communityPostRepository.countByCommunityIdAndIsDeletedFalse(community.getId()));
        community.setHeatScore((community.getHeatScore() == null ? 0L : community.getHeatScore()) + 3);
        communityRepository.save(community);

        return ResponseEntity.ok(ApiResponse.success("发布成功", convertPost(saved)));
    }

    @GetMapping("/posts/my")
    @Operation(summary = "我的帖子列表", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listMyPosts(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CommunityPost> posts = communityPostRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userDetails.getId(), pageable);

        CommunityPostPageDto pageDto = CommunityPostPageDto.builder()
                .items(posts.getContent().stream().map(this::convertPost).toList())
                .total(posts.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageDto));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "帖子详情")
    @Transactional
    public ResponseEntity<ApiResponse<CommunityPostDto>> getPost(@PathVariable Long postId) {
        CommunityPost post = findPost(postId);
        post.setViewCount((post.getViewCount() == null ? 0L : post.getViewCount()) + 1);
        CommunityPost saved = communityPostRepository.save(post);
        return ResponseEntity.ok(ApiResponse.success(convertPost(saved)));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "删除帖子", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CommunityPost post = findPost(postId);
        Community community = post.getCommunity();
        Long currentUserId = userDetails.getId();

        boolean isAuthor = post.getUser().getId().equals(currentUserId);
        boolean isCommunityOwner = community.getCreatorId() != null && community.getCreatorId().equals(currentUserId);

        if (!isAuthor && !isCommunityOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除此帖子"));
        }

        post.setIsDeleted(true);
        communityPostRepository.save(post);

        community.setPostCount(communityPostRepository.countByCommunityIdAndIsDeletedFalse(community.getId()));
        communityRepository.save(community);

        return ResponseEntity.ok(ApiResponse.success("帖子已删除"));
    }

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "点亮帖子", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<CommunityPostDto>> likePost(@PathVariable Long postId) {
        CommunityPost post = findPost(postId);
        post.setLikeCount((post.getLikeCount() == null ? 0L : post.getLikeCount()) + 1);
        post.getCommunity().setHeatScore((post.getCommunity().getHeatScore() == null ? 0L : post.getCommunity().getHeatScore()) + 1);
        CommunityPost saved = communityPostRepository.save(post);
        return ResponseEntity.ok(ApiResponse.success("已点亮", convertPost(saved)));
    }

    @GetMapping("/posts/{postId}/replies")
    @Operation(summary = "帖子回复列表")
    public ResponseEntity<ApiResponse<List<CommunityReplyDto>>> listReplies(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        List<CommunityReplyDto> replies = communityReplyRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable)
                .getContent()
                .stream()
                .map(this::convertReply)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @PostMapping("/posts/{postId}/replies")
    @Operation(summary = "回复帖子", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<CommunityReplyDto>> createReply(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CommunityPost post = findPost(postId);
        User user = findUser(userDetails.getId());
        syncPublicProfile(user, request.getDisplayName(), request.getAvatarUrl());

        CommunityReply reply = CommunityReply.builder()
                .post(post)
                .user(user)
                .content(request.getContent().trim())
                .likeCount(0L)
                .isDeleted(false)
                .build();
        CommunityReply saved = communityReplyRepository.save(reply);

        post.setReplyCount(communityReplyRepository.countByPostIdAndIsDeletedFalse(postId));
        post.setLastRepliedAt(LocalDateTime.now());
        post.getCommunity().setHeatScore((post.getCommunity().getHeatScore() == null ? 0L : post.getCommunity().getHeatScore()) + 2);
        communityPostRepository.save(post);

        return ResponseEntity.ok(ApiResponse.success("回复成功", convertReply(saved)));
    }

    @DeleteMapping("/replies/{replyId}")
    @Operation(summary = "删除回复", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CommunityReply reply = communityReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("回复", "id", replyId));

        CommunityPost post = reply.getPost();
        Community community = post.getCommunity();
        Long currentUserId = userDetails.getId();

        boolean isReplyAuthor = reply.getUser().getId().equals(currentUserId);
        boolean isPostAuthor = post.getUser().getId().equals(currentUserId);
        boolean isCommunityOwner = community.getCreatorId() != null && community.getCreatorId().equals(currentUserId);

        if (!isReplyAuthor && !isPostAuthor && !isCommunityOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除此回复"));
        }

        reply.setIsDeleted(true);
        communityReplyRepository.save(reply);

        post.setReplyCount(communityReplyRepository.countByPostIdAndIsDeletedFalse(post.getId()));
        communityPostRepository.save(post);

        return ResponseEntity.ok(ApiResponse.success("回复已删除"));
    }

    @PostMapping("/replies/{replyId}/like")
    @Operation(summary = "点亮帖子回复", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<CommunityReplyDto>> likeReply(@PathVariable Long replyId) {
        CommunityReply reply = communityReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子回复", "id", replyId));
        reply.setLikeCount((reply.getLikeCount() == null ? 0L : reply.getLikeCount()) + 1);
        CommunityReply saved = communityReplyRepository.save(reply);
        return ResponseEntity.ok(ApiResponse.success("已点亮", convertReply(saved)));
    }

    private Community findCommunity(String slug) {
        return communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("社区", "slug", slug));
    }

    private CommunityPost findPost(Long postId) {
        return communityPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("社区帖子", "id", postId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
    }

    private CommunityDto convertCommunity(Community community) {
        return CommunityDto.builder()
                .id(community.getId())
                .slug(community.getSlug())
                .name(community.getName())
                .description(community.getDescription())
                .coverImage(community.getCoverImage())
                .category(community.getCategory())
                .tags(splitTags(community.getTags()))
                .relatedAnimeId(community.getRelatedAnime() == null ? null : community.getRelatedAnime().getId())
                .relatedAnimeTitle(community.getRelatedAnime() == null ? null : community.getRelatedAnime().getTitle())
                .postCount(community.getPostCount())
                .memberCount(community.getMemberCount())
                .heatScore(community.getHeatScore())
                .featured(community.getFeatured())
                .creatorId(community.getCreatorId())
                .createdAt(community.getCreatedAt())
                .build();
    }

    private CommunityPostDto convertPost(CommunityPost post) {
        User user = post.getUser();
        Community community = post.getCommunity();
        return CommunityPostDto.builder()
                .id(post.getId())
                .communityId(community.getId())
                .communitySlug(community.getSlug())
                .communityName(community.getName())
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(resolvePublicDisplayName(user))
                .avatarUrl(user.getAvatarUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .images(parseImages(post.getImages()))
                .likeCount(post.getLikeCount())
                .replyCount(post.getReplyCount())
                .viewCount(post.getViewCount())
                .pinned(post.getPinned())
                .featured(post.getFeatured())
                .lastRepliedAt(post.getLastRepliedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private CommunityReplyDto convertReply(CommunityReply reply) {
        User user = reply.getUser();
        return CommunityReplyDto.builder()
                .id(reply.getId())
                .postId(reply.getPost().getId())
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(resolvePublicDisplayName(user))
                .avatarUrl(user.getAvatarUrl())
                .content(reply.getContent())
                .likeCount(reply.getLikeCount())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    private List<String> splitTags(String tags) {
        if (!hasText(tags)) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    private void syncPublicProfile(User user, String displayName, String avatarUrl) {
        boolean changed = false;
        if (isSafePublicName(displayName, user.getPhone()) && !displayName.trim().equals(user.getDisplayName())) {
            user.setDisplayName(displayName.trim());
            changed = true;
        }
        if (hasText(avatarUrl) && !avatarUrl.trim().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl.trim());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private String resolvePublicDisplayName(User user) {
        if (hasText(user.getDisplayName())) return user.getDisplayName();
        String username = user.getUsername();
        if (hasText(username) && !username.equals(user.getPhone()) && !username.matches("^1\\d{10}$")) {
            return username;
        }
        return "番舍同好";
    }

    private boolean isSafePublicName(String value, String phone) {
        if (!hasText(value)) return false;
        String trimmed = value.trim();
        return !"番舍同好".equals(trimmed)
                && !trimmed.equals(phone)
                && !trimmed.matches("^1\\d{10}$");
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> parseImages(String imagesJson) {
        if (!hasText(imagesJson)) return List.of();
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }
}
