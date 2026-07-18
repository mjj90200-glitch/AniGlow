package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.vote.FireflyVoteRequest;
import com.aniglow.dto.vote.FireflyVoteResponse;
import com.aniglow.entity.Anime;
import com.aniglow.entity.FireflyVote;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.FireflyVoteRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/votes")
@RequiredArgsConstructor
@Tag(name = "萤火投票", description = "番剧全站投票相关接口")
public class FireflyVoteController {

    private final FireflyVoteRepository fireflyVoteRepository;
    private final AnimeRepository animeRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "投出一张萤火票", description = "为番剧增加一张全站可见萤火票",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public ResponseEntity<ApiResponse<FireflyVoteResponse>> vote(
            @Valid @RequestBody FireflyVoteRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Anime anime = animeRepository.findById(request.getAnimeId())
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "id", request.getAnimeId()));
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userDetails.getId()));

        fireflyVoteRepository.save(FireflyVote.builder()
                .anime(anime)
                .user(user)
                .build());

        long animeVoteCount = fireflyVoteRepository.countByAnimeId(anime.getId());
        animeRepository.updateFireflyVoteCount(anime.getId(), animeVoteCount);

        FireflyVoteResponse response = FireflyVoteResponse.builder()
                .animeId(anime.getId())
                .fireflyVoteCount(animeVoteCount)
                .userVoteCount(fireflyVoteRepository.countByUserId(user.getId()))
                .build();

        return ResponseEntity.ok(ApiResponse.success("已投出一张萤火票", response));
    }
}
