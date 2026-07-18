package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.agent.AgentChatRequest;
import com.aniglow.dto.agent.AgentChatResponse;
import com.aniglow.dto.agent.AgentCharacterDto;
import com.aniglow.dto.agent.AgentMembershipStatusResponse;
import com.aniglow.dto.agent.AgentQuotaStatusResponse;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "AIGC 角色对话", description = "动漫角色 AIGC 对话接口")
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/roles")
    @Operation(summary = "获取可用角色列表")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAvailableRoles()));
    }

    @GetMapping("/characters")
    @Operation(summary = "获取 AIGC 角色配置", description = "返回启用角色的展示配置，供前端角色中心和后续音频能力使用")
    public ResponseEntity<ApiResponse<List<AgentCharacterDto>>> getCharacters() {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAvailableCharacters()));
    }

    @GetMapping("/characters/anime/{animeId}")
    @Operation(summary = "获取番剧关联 AIGC 角色", description = "根据番剧标题、译名和角色来源作品匹配可对话角色")
    public ResponseEntity<ApiResponse<List<AgentCharacterDto>>> getCharactersByAnime(
            @PathVariable Long animeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getCharactersByAnimeId(animeId)));
    }

    @PostMapping("/chat")
    @Operation(summary = "与角色对话（阻塞式）")
    public ResponseEntity<ApiResponse<AgentChatResponse>> chat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        AgentChatResponse response = agentService.chat(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/chat/stream")
    @Operation(summary = "与角色对话（SSE 流式）", description = "返回 text/event-stream，逐 token 推送 AI 回复")
    public SseEmitter chatStream(
            @Valid @RequestBody AgentChatRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return agentService.chatStream(request, user.getId());
    }

    @GetMapping("/membership/status")
    @Operation(summary = "获取萤火会员状态", description = "返回当前用户 AIGC 对话额度、月卡状态和支付二维码地址")
    public ResponseEntity<ApiResponse<AgentMembershipStatusResponse>> getMembershipStatus(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getMembershipStatus(user.getId())));
    }

    @GetMapping("/quota")
    @Operation(summary = "获取 AIGC 对话配额", description = "返回当前小时已用、剩余次数和下次整点重置时间")
    public ResponseEntity<ApiResponse<AgentQuotaStatusResponse>> getQuotaStatus(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getQuotaStatus(user.getId())));
    }

    @PostMapping("/membership/activate-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理端开通萤火月卡（开发期）", description = "仅供管理端占位；正式支付接入后迁移到支付宝服务端回调")
    public ResponseEntity<ApiResponse<AgentMembershipStatusResponse>> activateMonthly(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ResponseEntity.ok(ApiResponse.success("萤火月卡已开通", agentService.activateMonthlyMembership(user.getId())));
    }
}
