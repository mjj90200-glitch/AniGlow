package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.agent.AgentVoteRequest;
import com.aniglow.dto.agent.AgentVoteStatusResponse;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.AgentVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent/vote")
@RequiredArgsConstructor
@Tag(name = "AIGC角色投票", description = "每周候选角色投票接口")
public class AgentVoteController {

    private final AgentVoteService agentVoteService;

    @GetMapping("/candidates")
    @Operation(summary = "获取本周候选角色列表")
    public ResponseEntity<ApiResponse<AgentVoteStatusResponse>> getCandidates(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        AgentVoteStatusResponse status = userId != null
                ? agentVoteService.getStatus(userId)
                : AgentVoteStatusResponse.builder()
                        .hasVoted(false)
                        .candidates(agentVoteService.getCandidates())
                        .build();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping
    @Operation(summary = "投票给候选角色", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AgentVoteStatusResponse>> vote(
            @Valid @RequestBody AgentVoteRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            AgentVoteStatusResponse status = agentVoteService.vote(userDetails.getId(), request.getCandidateId());
            return ResponseEntity.ok(ApiResponse.success("投票成功", status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "查询当前用户投票状态", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AgentVoteStatusResponse>> getStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(agentVoteService.getStatus(userDetails.getId())));
    }
}
