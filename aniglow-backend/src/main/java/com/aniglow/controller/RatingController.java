package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.rating.RatingDto;
import com.aniglow.dto.rating.RatingRequest;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Tag(name = "评分", description = "动漫评分相关接口")
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/anime/{animeId}")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getRatingsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(ratingService.listByAnime(animeId, page, size));
    }

    @GetMapping("/anime/{animeId}/reviews")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getReviewsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(ratingService.listReviewsByAnime(animeId, page, size));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getRecentReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(ratingService.listRecent(page, size));
    }

    @PostMapping
    @Operation(summary = "提交评分", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingDto>> createRating(
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RatingService.MutationResult result = ratingService.save(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result.rating()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingDto>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "评分已更新", ratingService.update(id, userDetails.getId(), request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ratingService.delete(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("评分已删除", null));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
