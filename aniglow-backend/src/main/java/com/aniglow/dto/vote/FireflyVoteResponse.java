package com.aniglow.dto.vote;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FireflyVoteResponse {

    private Long animeId;
    private Long fireflyVoteCount;
    private Long userVoteCount;
}
