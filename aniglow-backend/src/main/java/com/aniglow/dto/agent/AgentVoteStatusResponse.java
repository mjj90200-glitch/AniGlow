package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVoteStatusResponse {
    private boolean hasVoted;
    private Long votedCandidateId;
    private List<AgentVoteCandidateDto> candidates;
}
