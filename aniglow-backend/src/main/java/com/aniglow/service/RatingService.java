package com.aniglow.service;

import com.aniglow.dto.rating.RatingDto;
import com.aniglow.dto.rating.RatingRequest;
import com.aniglow.entity.Anime;
import com.aniglow.entity.Rating;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.RatingMapper;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final AnimeRepository animeRepository;
    private final UserRepository userRepository;
    private final BayesianRatingService bayesianRatingService;
    private final UserProfileService userProfileService;
    private final RatingMapper ratingMapper;
    private final AnimeCacheInvalidator cacheInvalidator;

    @Transactional(readOnly = true)
    public List<RatingDto> listByAnime(Long animeId, int page, int size) {
        return ratingRepository.findByAnimeIdOrderByCreatedAtDesc(animeId, page(page, size))
                .getContent().stream().map(ratingMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RatingDto> listReviewsByAnime(Long animeId, int page, int size) {
        return ratingRepository.findPopularReviewsByAnimeId(animeId, page(page, size))
                .getContent().stream().map(ratingMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RatingDto> listRecent(int page, int size) {
        return ratingRepository.findRecentReviews(page(page, size))
                .getContent().stream().map(ratingMapper::toDto).toList();
    }

    @Transactional
    public MutationResult save(Long userId, RatingRequest request) {
        Anime anime = findAnime(request.getAnimeId());
        User user = findUser(userId);
        Rating rating = ratingRepository.findByUserIdAndAnimeId(userId, anime.getId()).orElse(null);
        boolean created = rating == null;
        if (created) {
            rating = Rating.builder().user(user).anime(anime).likeCount(0L).build();
        }
        userProfileService.syncPublicProfile(rating.getUser(), request.getDisplayName(), request.getAvatarUrl());
        apply(rating, request);
        Rating saved = ratingRepository.save(rating);
        bayesianRatingService.updateCommunityRatingStats(anime.getId());
        cacheInvalidator.invalidateReadModels();
        return new MutationResult(created ? "评分成功" : "评分已更新", ratingMapper.toDto(saved));
    }

    @Transactional
    public RatingDto update(Long id, Long userId, RatingRequest request) {
        Rating rating = findRating(id);
        requireOwner(rating, userId);
        userProfileService.syncPublicProfile(rating.getUser(), request.getDisplayName(), request.getAvatarUrl());
        apply(rating, request);
        Rating saved = ratingRepository.save(rating);
        bayesianRatingService.updateCommunityRatingStats(rating.getAnime().getId());
        cacheInvalidator.invalidateReadModels();
        return ratingMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Rating rating = findRating(id);
        requireOwner(rating, userId);
        Long animeId = rating.getAnime().getId();
        ratingRepository.delete(rating);
        ratingRepository.flush();
        bayesianRatingService.updateCommunityRatingStats(animeId);
        cacheInvalidator.invalidateReadModels();
    }

    private void apply(Rating rating, RatingRequest request) {
        rating.setScore(decimal(request.getScore()));
        rating.setReview(request.getReview());
        rating.setStoryScore(decimal(request.getStoryScore()));
        rating.setAnimationScore(decimal(request.getAnimationScore()));
        rating.setSoundScore(decimal(request.getSoundScore()));
        rating.setCharacterScore(decimal(request.getCharacterScore()));
        rating.setEnjoymentScore(decimal(request.getEnjoymentScore()));
        rating.setIsRecommended(request.getIsRecommended());
        rating.setContainsSpoiler(request.getContainsSpoiler());
    }

    private Rating findRating(Long id) {
        return ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评分", "id", id));
    }

    private Anime findAnime(Long id) {
        return animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "id", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", id));
    }

    private void requireOwner(Rating rating, Long userId) {
        if (!rating.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("无权操作他人评分");
        }
    }

    private PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("分页参数无效");
        }
        return PageRequest.of(page, size);
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    public record MutationResult(String message, RatingDto rating) {}
}
