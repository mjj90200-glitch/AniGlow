package com.aniglow.mapper;

import com.aniglow.dto.anime.AnimeDto;
import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.entity.Anime;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AnimeMapper {

    public AnimeDto toDto(Anime anime) {
        return AnimeDto.builder()
                .id(anime.getId())
                .malId(anime.getMalId())
                .title(anime.getTitle())
                .titleJapanese(anime.getTitleJapanese())
                .titleEnglish(anime.getTitleEnglish())
                .titleCn(anime.getTitleCn())
                .searchAliases(anime.getSearchAliases())
                .synopsis(anime.getSynopsis())
                .synopsisCn(anime.getSynopsisCn())
                .coverImage(anime.getCoverImage())
                .trailerUrl(anime.getTrailerUrl())
                .type(anime.getType())
                .status(anime.getStatus())
                .airedFrom(anime.getAiredFrom())
                .airedTo(anime.getAiredTo())
                .episodes(anime.getEpisodes())
                .durationMinutes(anime.getDurationMinutes())
                .rating(anime.getRating())
                .popularity(anime.getPopularity())
                .membersCount(anime.getMembersCount())
                .favoritesCount(anime.getFavoritesCount())
                .hasAgent(Boolean.TRUE.equals(anime.getHasAgent()))
                .bayesianRating(number(anime.getBayesianRating()))
                .meanRating(number(anime.getMeanRating()))
                .ratingCount(anime.getRatingCount())
                .communityBayesianRating(number(anime.getCommunityBayesianRating()))
                .communityMeanRating(number(anime.getCommunityMeanRating()))
                .communityRatingCount(anime.getCommunityRatingCount() == null ? 0L : anime.getCommunityRatingCount())
                .fireflyVoteCount(anime.getFireflyVoteCount() == null ? 0L : anime.getFireflyVoteCount())
                .genres(anime.getGenres() == null ? Set.of() : Set.copyOf(anime.getGenres()))
                .studio(anime.getStudio())
                .source(anime.getSource())
                .season(anime.getSeason())
                .year(anime.getYear())
                .createdAt(anime.getCreatedAt())
                .updatedAt(anime.getUpdatedAt())
                .build();
    }

    public AnimeListResponse toListResponse(Page<Anime> page) {
        return AnimeListResponse.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Double number(Number value) {
        return value == null ? null : value.doubleValue();
    }
}
