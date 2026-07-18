package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.rating.RatingReplyDto;
import com.aniglow.dto.rating.RatingReplyRequest;
import com.aniglow.entity.Rating;
import com.aniglow.entity.RatingReply;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.RatingReplyRepository;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "评论回复", description = "评分评论的楼中楼回复接口")
public class RatingReplyController {

    private final RatingRepository ratingRepository;
    private final RatingReplyRepository ratingReplyRepository;
    private final UserRepository userRepository;

    @GetMapping("/ratings/{ratingId}/replies")
    @Operation(summary = "获取评论回复", description = "获取指定评分评论下的回复")
    public ResponseEntity<ApiResponse<List<RatingReplyDto>>> getReplies(
            @PathVariable Long ratingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        List<RatingReplyDto> replies = ratingReplyRepository
                .findByRatingIdOrderByCreatedAtAsc(ratingId, pageable)
                .getContent()
                .stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @PostMapping("/ratings/{ratingId}/replies")
    @Operation(summary = "发布评论回复", description = "回复某条评分评论（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingReplyDto>> createReply(
            @PathVariable Long ratingId,
            @Valid @RequestBody RatingReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("评分评论", "id", ratingId));
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userDetails.getId()));

        syncPublicProfile(user, request);

        RatingReply reply = RatingReply.builder()
                .rating(rating)
                .user(user)
                .content(request.getContent().trim())
                .likeCount(0L)
                .build();

        RatingReply saved = ratingReplyRepository.save(reply);
        return ResponseEntity.ok(ApiResponse.success("回复成功", convertToDto(saved)));
    }

    @PostMapping("/rating-replies/{replyId}/like")
    @Operation(summary = "点亮评论回复", description = "为一条楼中楼回复增加点亮数（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingReplyDto>> likeReply(@PathVariable Long replyId) {
        RatingReply reply = ratingReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("评论回复", "id", replyId));
        reply.setLikeCount((reply.getLikeCount() == null ? 0L : reply.getLikeCount()) + 1);
        RatingReply saved = ratingReplyRepository.save(reply);
        return ResponseEntity.ok(ApiResponse.success("已点亮", convertToDto(saved)));
    }

    @DeleteMapping("/rating-replies/{replyId}")
    @Operation(summary = "删除自己的评论回复", description = "删除当前用户发布的回复（需要登录）",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        RatingReply reply = ratingReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("评论回复", "id", replyId));
        if (!reply.getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除他人的回复"));
        }

        ratingReplyRepository.delete(reply);
        return ResponseEntity.ok(ApiResponse.success("回复已删除", null));
    }

    private RatingReplyDto convertToDto(RatingReply reply) {
        User user = reply.getUser();
        return RatingReplyDto.builder()
                .id(reply.getId())
                .ratingId(reply.getRating().getId())
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

    private void syncPublicProfile(User user, RatingReplyRequest request) {
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

    private String resolvePublicDisplayName(User user) {
        if (hasText(user.getDisplayName())) return user.getDisplayName();

        String username = user.getUsername();
        String phone = user.getPhone();
        if (hasText(username) && !username.equals(phone) && !username.matches("^1\\d{10}$")) {
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
