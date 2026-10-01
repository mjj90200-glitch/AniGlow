package com.aniglow.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CommunityPostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不能超过 120 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容不能超过 5000 字")
    private String content;

    @Size(max = 500)
    private String coverImage;

    @Size(max = 9, message = "每篇帖子最多上传 9 张图片")
    private List<@Size(max = 500, message = "图片地址不能超过 500 字") String> images;

    @Size(max = 80)
    private String displayName;

    @Size(max = 2_000_000)
    private String avatarUrl;
}
