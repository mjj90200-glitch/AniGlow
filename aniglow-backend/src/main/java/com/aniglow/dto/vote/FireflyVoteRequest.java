package com.aniglow.dto.vote;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FireflyVoteRequest {

    @NotNull(message = "动漫ID不能为空")
    private Long animeId;
}
