package com.aniglow.dto.agent;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentVoteRequest {
    @NotNull
    private Long candidateId;
}
