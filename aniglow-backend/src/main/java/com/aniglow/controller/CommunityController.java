package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.community.*;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.CommunityService;
import com.aniglow.service.ImageStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final ImageStorageService imageStorageService;

    @PostMapping("/upload/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(ApiResponse.success("上传成功", imageStorageService.storeImages(files)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityDto>>> listCommunities() {
        return ok(communityService.listCommunities());
    }

    @GetMapping("/posts/by-anime/{animeId}")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listPostsByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ok(communityService.listPostsByAnime(animeId, page, size));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CommunityDto>> getCommunity(@PathVariable String slug) {
        return ok(communityService.getCommunity(slug));
    }

    @GetMapping("/{slug}/posts")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(communityService.listPosts(slug, sort, page, size));
    }

    @PostMapping("/{slug}/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityPostDto>> createPost(
            @PathVariable String slug,
            @Valid @RequestBody CommunityPostRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "发布成功", communityService.createPost(slug, userDetails.getId(), request)));
    }

    @GetMapping("/posts/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityPostPageDto>> listMyPosts(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(communityService.listMyPosts(userDetails.getId(), page, size));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostDto>> getPost(@PathVariable Long postId) {
        return ok(communityService.getPost(postId));
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        communityService.deletePost(postId, userDetails.getId());
        return ok("帖子已删除");
    }

    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityPostDto>> likePost(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("已点亮", communityService.likePost(postId)));
    }

    @GetMapping("/posts/{postId}/replies")
    public ResponseEntity<ApiResponse<List<CommunityReplyDto>>> listReplies(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ok(communityService.listReplies(postId, page, size));
    }

    @PostMapping("/posts/{postId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityReplyDto>> createReply(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "回复成功", communityService.createReply(postId, userDetails.getId(), request)));
    }

    @DeleteMapping("/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteReply(
            @PathVariable Long replyId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        communityService.deleteReply(replyId, userDetails.getId());
        return ok("回复已删除");
    }

    @PostMapping("/replies/{replyId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityReplyDto>> likeReply(@PathVariable Long replyId) {
        return ResponseEntity.ok(ApiResponse.success("已点亮", communityService.likeReply(replyId)));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
