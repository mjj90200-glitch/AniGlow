package com.aniglow.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostPageDto {

    private List<CommunityPostDto> items;
    private long total;
    private int page;
    private int size;
}
