package com.aniglow.service;

import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.entity.Anime;
import com.aniglow.mapper.AnimeMapper;
import com.aniglow.repository.AnimeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimeServiceTest {

    private final AnimeRepository repository = mock(AnimeRepository.class);
    private final AnimeMapper mapper = mock(AnimeMapper.class);
    private final AnimeService service = new AnimeService(repository, mock(BayesianRatingService.class), mapper);

    @Test
    void usesFullTextSearchWhenItFindsResults() {
        ReflectionTestUtils.setField(service, "fulltextEnabled", true);
        PageRequest page = PageRequest.of(0, 20);
        Page<Anime> result = new PageImpl<>(List.of(Anime.builder().id(1L).build()), page, 1);
        AnimeListResponse response = AnimeListResponse.builder().totalElements(1).build();
        when(repository.searchByFullText("芙莉莲*", page)).thenReturn(result);
        when(mapper.toListResponse(result)).thenReturn(response);

        assertThat(service.search("芙莉莲", 0, 20)).isSameAs(response);
        verify(repository, never()).searchByTitle("芙莉莲", page);
    }

    @Test
    void FallsBackToLikeForPartialChineseTitles() {
        ReflectionTestUtils.setField(service, "fulltextEnabled", true);
        PageRequest page = PageRequest.of(0, 20);
        Page<Anime> empty = Page.empty(page);
        Page<Anime> fallback = new PageImpl<>(List.of(Anime.builder().id(2L).build()), page, 1);
        when(repository.searchByFullText("鬼灭*", page)).thenReturn(empty);
        when(repository.searchByTitle("鬼灭", page)).thenReturn(fallback);
        when(mapper.toListResponse(fallback)).thenReturn(AnimeListResponse.builder().totalElements(1).build());

        assertThat(service.search("鬼灭", 0, 20).getTotalElements()).isEqualTo(1);
        verify(repository).searchByTitle("鬼灭", page);
    }
}
