package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.repository.AgentCharacterRepository;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "公开统计", description = "首页展示用的真实统计数据")
public class StatsController {

    private final AnimeRepository animeRepository;
    private final AgentCharacterRepository agentCharacterRepository;
    private final UserRepository userRepository;

    @GetMapping("/public")
    @Operation(summary = "首页公开统计", description = "返回真实收录番剧、启用 Agent 和社区成员数量")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPublicStats() {
        Map<String, Long> stats = Map.of(
                "animeCount", animeRepository.count(),
                "agentCount", agentCharacterRepository.countByEnabledTrue(),
                "memberCount", userRepository.count()
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
