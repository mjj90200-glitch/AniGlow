package com.aniglow.dto.community;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 管理员专用展示数据调整请求。仅对应 /admin/** 端点，不参与普通用户互动。
 */
@Data
public class CommunityStatsOverrideRequest {

    @PositiveOrZero(message = "点赞数不能为负数")
    private Long likeCount;

    @PositiveOrZero(message = "浏览数不能为负数")
    private Long viewCount;

    @PositiveOrZero(message = "成员数不能为负数")
    private Long memberCount;

    @PositiveOrZero(message = "热度不能为负数")
    private Long heatScore;
}
