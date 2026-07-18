package com.aniglow.service;

import com.aniglow.dto.rating.RatingDto;
import com.aniglow.entity.Rating;
import com.aniglow.mapper.RatingMapper;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RatingServiceTest {

    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final RatingMapper ratingMapper = mock(RatingMapper.class);
    private final RatingService service = new RatingService(
            ratingRepository,
            mock(AnimeRepository.class),
            mock(UserRepository.class),
            mock(BayesianRatingService.class),
            mock(UserProfileService.class),
            ratingMapper,
            mock(AnimeCacheInvalidator.class)
    );

    @Test
    void delegatesAnimeReviewsToDatabasePagination() {
        Rating rating = Rating.builder().id(7L).build();
        RatingDto dto = RatingDto.builder().id(7L).build();
        PageRequest page = PageRequest.of(2, 20);
        when(ratingRepository.findByAnimeIdOrderByCreatedAtDesc(9L, page))
                .thenReturn(new PageImpl<>(List.of(rating), page, 41));
        when(ratingMapper.toDto(rating)).thenReturn(dto);

        assertThat(service.listByAnime(9L, 2, 20)).containsExactly(dto);
        verify(ratingRepository).findByAnimeIdOrderByCreatedAtDesc(9L, page);
    }

    @Test
    void rejectsUnboundedPageSizes() {
        assertThatThrownBy(() -> service.listByAnime(9L, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分页");
    }
}
