package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.rating.RatingDto;
import com.aniglow.dto.rating.RatingRequest;
import com.aniglow.entity.Anime;
import com.aniglow.entity.Rating;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.BayesianRatingService;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Tag(name = "评分", description = "动漫评分相关接口")
public class RatingController {

    private final RatingRepository ratingRepository;
    private final AnimeRepository animeRepository;
    private final UserRepository userRepository;
    private final BayesianRatingService bayesianRatingService;

    @GetMapping("/anime/{animeId}")
    @Operation(summary = "获取动漫评分列表", description = "获取指定动漫的所有评分")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getRatingsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        List<Rating> ratings = ratingRepository.findByAnimeId(animeId);

        List<RatingDto> dtos = ratings.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/anime/{animeId}/reviews")
    @Operation(summary = "获取动漫评论", description = "获取带评论的评分")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getReviewsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Rating> ratingPage = ratingRepository.findPopularReviewsByAnimeId(animeId, pageable);

        List<RatingDto> dtos = ratingPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/recent")
    @Operation(summary = "最新评论", description = "获取全站最新评论")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getRecentReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Rating> ratingPage = ratingRepository.findRecentReviews(pageable);

        List<RatingDto> dtos = ratingPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping
    @Operation(summary = "提交评分", description = "为动漫评分（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingDto>> createRating(
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // 检查动漫是否存在
        Anime anime = animeRepository.findById(request.getAnimeId())
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "id", request.getAnimeId()));

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userDetails.getId()));

        return ratingRepository.findByUserIdAndAnimeId(userDetails.getId(), request.getAnimeId())
                .map(existingRating -> updateExistingRating(existingRating, request, anime.getId()))
                .orElseGet(() -> createNewRating(request, user, anime));
    }

    private ResponseEntity<ApiResponse<RatingDto>> createNewRating(RatingRequest request, User user, Anime anime) {
        syncPublicProfile(user, request);

        Rating rating = Rating.builder()
                .user(user)
                .anime(anime)
                .likeCount(0L)
                .build();
        applyRatingRequest(rating, request);

        Rating savedRating = ratingRepository.save(rating);

        // 更新站内用户评分统计，不覆盖抓取得到的原始评分排行
        bayesianRatingService.updateCommunityRatingStats(anime.getId());

        return ResponseEntity.ok(ApiResponse.success("评分成功", convertToDto(savedRating)));
    }

    private ResponseEntity<ApiResponse<RatingDto>> updateExistingRating(Rating rating, RatingRequest request, Long animeId) {
        syncPublicProfile(rating.getUser(), request);

        applyRatingRequest(rating, request);
        Rating updatedRating = ratingRepository.save(rating);

        // 同一用户同一番剧再次发布时视为更新自己的评分/评论，避免前端出现“发布了但没保存”的错觉。
        bayesianRatingService.updateCommunityRatingStats(animeId);

        return ResponseEntity.ok(ApiResponse.success("评分已更新", convertToDto(updatedRating)));
    }

    private void applyRatingRequest(Rating rating, RatingRequest request) {
        rating.setScore(request.getScore() != null ? BigDecimal.valueOf(request.getScore()) : null);
        rating.setReview(request.getReview());
        rating.setStoryScore(request.getStoryScore() != null ? BigDecimal.valueOf(request.getStoryScore()) : null);
        rating.setAnimationScore(request.getAnimationScore() != null ? BigDecimal.valueOf(request.getAnimationScore()) : null);
        rating.setSoundScore(request.getSoundScore() != null ? BigDecimal.valueOf(request.getSoundScore()) : null);
        rating.setCharacterScore(request.getCharacterScore() != null ? BigDecimal.valueOf(request.getCharacterScore()) : null);
        rating.setEnjoymentScore(request.getEnjoymentScore() != null ? BigDecimal.valueOf(request.getEnjoymentScore()) : null);
        rating.setIsRecommended(request.getIsRecommended());
        rating.setContainsSpoiler(request.getContainsSpoiler());
    }

    private void syncPublicProfile(User user, RatingRequest request) {
        boolean changed = false;
        String displayName = request.getDisplayName();
        if (isSafePublicName(displayName, user.getPhone()) && !displayName.trim().equals(user.getDisplayName())) {
            user.setDisplayName(displayName.trim());
            changed = true;
        }

        String avatarUrl = request.getAvatarUrl();
        if (hasText(avatarUrl) && !avatarUrl.trim().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl.trim());
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新评分", description = "修改已有评分（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingDto>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评分", "id", id));

        // 验证权限
        if (!rating.getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权修改他人评分"));
        }

        syncPublicProfile(rating.getUser(), request);

        rating.setScore(request.getScore() != null ? BigDecimal.valueOf(request.getScore()) : null);
        rating.setReview(request.getReview());
        rating.setStoryScore(request.getStoryScore() != null ? BigDecimal.valueOf(request.getStoryScore()) : null);
        rating.setAnimationScore(request.getAnimationScore() != null ? BigDecimal.valueOf(request.getAnimationScore()) : null);
        rating.setSoundScore(request.getSoundScore() != null ? BigDecimal.valueOf(request.getSoundScore()) : null);
        rating.setCharacterScore(request.getCharacterScore() != null ? BigDecimal.valueOf(request.getCharacterScore()) : null);
        rating.setEnjoymentScore(request.getEnjoymentScore() != null ? BigDecimal.valueOf(request.getEnjoymentScore()) : null);
        rating.setIsRecommended(request.getIsRecommended());
        rating.setContainsSpoiler(request.getContainsSpoiler());

        Rating updatedRating = ratingRepository.save(rating);

        // 更新站内用户评分统计，不覆盖抓取得到的原始评分排行
        bayesianRatingService.updateCommunityRatingStats(rating.getAnime().getId());

        return ResponseEntity.ok(ApiResponse.success("评分已更新", convertToDto(updatedRating)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评分", description = "删除评分（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评分", "id", id));

        // 验证权限
        if (!rating.getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除他人评分"));
        }

        Long animeId = rating.getAnime().getId();
        ratingRepository.delete(rating);

        // 更新站内用户评分统计，不覆盖抓取得到的原始评分排行
        bayesianRatingService.updateCommunityRatingStats(animeId);

        return ResponseEntity.ok(ApiResponse.success("评分已删除", null));
    }

    private RatingDto convertToDto(Rating rating) {
        User user = rating.getUser();
        String displayName = resolvePublicDisplayName(user);
        return RatingDto.builder()
                .id(rating.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(displayName)
                .avatarUrl(user.getAvatarUrl())
                .animeId(rating.getAnime().getId())
                .animeTitle(rating.getAnime().getTitle())
                .animeCoverImage(rating.getAnime().getCoverImage())
                .score(rating.getScore() != null ? rating.getScore().doubleValue() : null)
                .review(rating.getReview())
                .storyScore(rating.getStoryScore() != null ? rating.getStoryScore().doubleValue() : null)
                .animationScore(rating.getAnimationScore() != null ? rating.getAnimationScore().doubleValue() : null)
                .soundScore(rating.getSoundScore() != null ? rating.getSoundScore().doubleValue() : null)
                .characterScore(rating.getCharacterScore() != null ? rating.getCharacterScore().doubleValue() : null)
                .enjoymentScore(rating.getEnjoymentScore() != null ? rating.getEnjoymentScore().doubleValue() : null)
                .isRecommended(rating.getIsRecommended())
                .containsSpoiler(rating.getContainsSpoiler())
                .likeCount(rating.getLikeCount())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    private String resolvePublicDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }

        String username = user.getUsername();
        String phone = user.getPhone();
        if (username != null
                && !username.isBlank()
                && !username.equals(phone)
                && !username.matches("^1\\d{10}$")) {
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
