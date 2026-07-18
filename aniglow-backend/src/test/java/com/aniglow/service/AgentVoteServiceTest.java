package com.aniglow.service;

import com.aniglow.entity.AgentVote;
import com.aniglow.entity.AgentVoteCandidate;
import com.aniglow.repository.AgentCharacterRepository;
import com.aniglow.repository.AgentVoteCandidateRepository;
import com.aniglow.repository.AgentVoteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentVoteServiceTest {

    private final AgentVoteRepository voteRepository = mock(AgentVoteRepository.class);
    private final AgentVoteCandidateRepository candidateRepository = mock(AgentVoteCandidateRepository.class);
    private final AgentCharacterRepository characterRepository = mock(AgentCharacterRepository.class);
    private final AgentVoteService service = new AgentVoteService(voteRepository, candidateRepository, characterRepository);

    @Test
    void recordsOneVoteAndIncrementsCandidate() {
        String week = AgentVoteService.currentWeekKey();
        AgentVoteCandidate candidate = AgentVoteCandidate.builder()
                .id(3L).code("Miku").displayName("三玖").weekKey(week).voteCount(0L).build();
        when(voteRepository.findByUserIdAndWeekKey(9L, week))
                .thenReturn(Optional.empty(), Optional.of(AgentVote.builder().candidate(candidate).build()));
        when(candidateRepository.findById(3L)).thenReturn(Optional.of(candidate));
        when(candidateRepository.findByWeekKeyOrderByVoteCountDesc(week)).thenReturn(List.of(candidate));

        var result = service.vote(9L, 3L);

        assertThat(result.isHasVoted()).isTrue();
        assertThat(candidate.getVoteCount()).isEqualTo(1L);
        verify(voteRepository).save(any(AgentVote.class));
    }

    @Test
    void doesNotRecordASecondVoteInTheSameWeek() {
        String week = AgentVoteService.currentWeekKey();
        AgentVoteCandidate candidate = AgentVoteCandidate.builder().id(3L).weekKey(week).build();
        when(voteRepository.findByUserIdAndWeekKey(9L, week)).thenReturn(Optional.of(
                AgentVote.builder().candidate(candidate).build()));
        when(candidateRepository.findByWeekKeyOrderByVoteCountDesc(week)).thenReturn(List.of(candidate));

        service.vote(9L, 3L);

        verify(voteRepository, never()).save(any());
        verify(candidateRepository, never()).save(any());
    }

    @Test
    void rejectsCandidateFromAnotherWeek() {
        String week = AgentVoteService.currentWeekKey();
        when(voteRepository.findByUserIdAndWeekKey(9L, week)).thenReturn(Optional.empty());
        when(candidateRepository.findById(3L)).thenReturn(Optional.of(
                AgentVoteCandidate.builder().id(3L).weekKey("2020-W01").build()));

        assertThatThrownBy(() -> service.vote(9L, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("本周");
    }
}
