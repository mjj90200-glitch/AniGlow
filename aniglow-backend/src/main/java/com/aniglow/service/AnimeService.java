package com.aniglow.service;

import com.aniglow.dto.anime.AnimeDto;
import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.entity.Anime;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.AnimeMapper;
import com.aniglow.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of(
            "bayesianRating", "communityBayesianRating", "fireflyVoteCount", "popularity",
            "createdAt", "updatedAt", "title", "year"
    );

    private final AnimeRepository animeRepository;
    private final BayesianRatingService bayesianRatingService;
    private final AnimeMapper animeMapper;

    @Value("${aniglow.search.fulltext-enabled:true}")
    private boolean fulltextEnabled;

    @Cacheable(value = "animeList", key = "'all:' + #page + ':' + #size + ':' + #sortBy + ':' + #direction")
    public AnimeListResponse list(int page, int size, String sortBy, String direction) {
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("不支持的排序字段: " + sortBy);
        }
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return animeMapper.toListResponse(animeRepository.findAll(pageable(page, size, sort)));
    }

    /** 综合筛选：关键词/题材/类型/年份区间/国家全部可选组合，按贝叶斯评分降序（NULL 在后） */
    public AnimeListResponse filter(String keyword, String genre, String type,
                                    Integer yearFrom, Integer yearTo, String country, int page, int size) {
        String kw = hasText(keyword) ? keyword.toLowerCase() : null;
        String g = hasText(genre) ? genre : null;
        String t = hasText(type) ? type : null;
        String c = hasText(country) ? country : null;
        Page<Anime> result = animeRepository.filterAnime(kw, g, t, yearFrom, yearTo, c,
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)));
        return animeMapper.toListResponse(result);
    }

    /** 筛选选项集（年份/地区/类型/题材均来自库内真实数据） */
    public Map<String, Object> filterOptions() {
        return Map.of(
                "years", animeRepository.findDistinctYears(),
                "countries", animeRepository.findDistinctCountries(),
                "types", animeRepository.findDistinctTypes(),
                "genres", animeRepository.findDistinctGenres()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Cacheable(value = "animeDetail", key = "#id")
    public AnimeDto getById(Long id) {
        return animeMapper.toDto(findAnime(id));
    }

    @Cacheable(value = "animeDetail", key = "'mal:' + #malId")
    public AnimeDto getByMalId(Long malId) {
        Anime anime = animeRepository.findByMalId(malId)
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "malId", malId));
        return animeMapper.toDto(anime);
    }

    @Cacheable(value = "animeList", key = "'search:' + #keyword + ':' + #page + ':' + #size")
    public AnimeListResponse search(String keyword, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        Pageable pageable = pageable(page, size);
        Page<Anime> result = normalized.isBlank()
                ? animeRepository.findAll(pageable)
                : fulltextEnabled
                        ? animeRepository.searchByFullText(toBooleanModeQuery(normalized), pageable)
                        : animeRepository.searchByTitle(normalized, pageable);
        // Chinese partial titles are not always tokenized by the default MySQL parser.
        if (fulltextEnabled && !normalized.isBlank() && result.isEmpty()) {
            result = animeRepository.searchByTitle(normalized, pageable);
        }
        return animeMapper.toListResponse(result);
    }

    @Cacheable(value = "ranking", key = "'source:' + #page + ':' + #size")
    public AnimeListResponse topRated(int page, int size) {
        return animeMapper.toListResponse(animeRepository.findAllByOrderByBayesianRatingDesc(pageable(page, size)));
    }

    @Cacheable(value = "ranking", key = "'community:' + #page + ':' + #size")
    public AnimeListResponse communityRated(int page, int size) {
        return animeMapper.toListResponse(animeRepository
                .findByCommunityRatingCountGreaterThanOrderByCommunityBayesianRatingDescFireflyVoteCountDesc(
                        0L, pageable(page, size)));
    }

    @Cacheable(value = "ranking", key = "'firefly:' + #page + ':' + #size")
    public AnimeListResponse fireflyRanking(int page, int size) {
        return animeMapper.toListResponse(animeRepository
                .findByFireflyVoteCountGreaterThanOrderByFireflyVoteCountDescCommunityBayesianRatingDesc(
                        0L, pageable(page, size)));
    }

    @Cacheable(value = "animeList", key = "'genre:' + #genre + ':' + #page + ':' + #size")
    public AnimeListResponse byGenre(String genre, int page, int size) {
        return animeMapper.toListResponse(animeRepository.findByGenresInOrderByBayesianRatingDesc(
                List.of(genre), pageable(page, size)));
    }

    @Cacheable(value = "animeSeason", key = "#year + ':' + #season")
    public List<AnimeDto> bySeason(Integer year, String season) {
        if (season == null || season.isBlank()) {
            throw new IllegalArgumentException("季度不能为空");
        }
        String normalized = season.substring(0, 1).toUpperCase() + season.substring(1).toLowerCase();
        return animeRepository.findByYearAndSeasonOrderByBayesianRatingDesc(year, normalized + " " + year)
                .stream().map(animeMapper::toDto).toList();
    }

    public double[] ratingDistribution(Long animeId) {
        if (!animeRepository.existsById(animeId)) {
            throw new ResourceNotFoundException("动漫", "id", animeId);
        }
        return bayesianRatingService.getRatingDistribution(animeId);
    }

    private Anime findAnime(Long id) {
        return animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "id", id));
    }

    private Pageable pageable(int page, int size) {
        return pageable(page, size, Sort.unsorted());
    }

    private Pageable pageable(int page, int size, Sort sort) {
        if (page < 0) throw new IllegalArgumentException("页码不能小于 0");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        return PageRequest.of(page, size, sort);
    }

    private String toBooleanModeQuery(String keyword) {
        String sanitized = keyword.replaceAll("[+><()~*\"@-]+", " ").trim();
        if (sanitized.isBlank()) return keyword;
        return String.join(" ", sanitized.split("\\s+")).replace(" ", "* ") + "*";
    }
}
