package com.aniglow.dto.anime;

import lombok.Data;

import java.util.List;

@Data
public class AnimeSearchRequest {

    private String keyword;
    private List<String> genres;
    private String type;
    private String status;
    private Integer year;
    private String season;
    private String sortBy;
    private String sortDirection;
}
