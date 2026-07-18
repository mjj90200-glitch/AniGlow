package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.rating.RatingReplyDto;
import com.aniglow.dto.rating.RatingReplyRequest;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.RatingReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RatingReplyController {

    private final RatingReplyService replyService;

    @GetMapping("/ratings/{ratingId}/replies")
    public ResponseEntity<ApiResponse<List<RatingReplyDto>>> getReplies(
            @PathVariable Long ratingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(replyService.list(ratingId, page, size)));
    }

    @PostMapping("/ratings/{ratingId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingReplyDto>> createReply(
            @PathVariable Long ratingId,
            @Valid @RequestBody RatingReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "回复成功", replyService.create(ratingId, userDetails.getId(), request)));
    }

    @PostMapping("/rating-replies/{replyId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingReplyDto>> likeReply(@PathVariable Long replyId) {
        return ResponseEntity.ok(ApiResponse.success("已点亮", replyService.like(replyId)));
    }

    @DeleteMapping("/rating-replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        replyService.delete(replyId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("回复已删除", null));
    }
}
