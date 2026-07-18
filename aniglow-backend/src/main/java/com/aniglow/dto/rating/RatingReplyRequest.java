package com.aniglow.dto.rating;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 800, message = "回复内容不能超过 800 字")
    private String content;

    @Size(max = 80)
    private String displayName;

    @Size(max = 2_000_000)
    private String avatarUrl;
}
