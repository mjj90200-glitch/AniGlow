package com.aniglow.mapper;

import com.aniglow.dto.community.CommunityDto;
import com.aniglow.dto.community.CommunityPostDto;
import com.aniglow.dto.community.CommunityReplyDto;
import com.aniglow.entity.Community;
import com.aniglow.entity.CommunityPost;
import com.aniglow.entity.CommunityReply;
import com.aniglow.entity.User;
import com.aniglow.service.UserDisplayNameResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommunityMapper {

    private final ObjectMapper objectMapper;
    private final UserDisplayNameResolver displayNameResolver;

    public CommunityDto toDto(Community community) {
        return CommunityDto.builder()
                .id(community.getId()).slug(community.getSlug()).name(community.getName())
                .description(community.getDescription()).coverImage(community.getCoverImage())
                .category(community.getCategory()).tags(splitTags(community.getTags()))
                .relatedAnimeId(community.getRelatedAnime() == null ? null : community.getRelatedAnime().getId())
                .relatedAnimeTitle(community.getRelatedAnime() == null ? null : community.getRelatedAnime().getTitle())
                .postCount(community.getPostCount()).memberCount(community.getMemberCount())
                .heatScore(community.getHeatScore()).featured(community.getFeatured())
                .creatorId(community.getCreatorId()).createdAt(community.getCreatedAt()).build();
    }

    public CommunityPostDto toPostDto(CommunityPost post) {
        return toPostDto(post, false);
    }

    public CommunityPostDto toPostDto(CommunityPost post, boolean likedByMe) {
        User user = post.getUser();
        Community community = post.getCommunity();
        return CommunityPostDto.builder()
                .id(post.getId()).communityId(community.getId()).communitySlug(community.getSlug())
                .communityName(community.getName()).userId(user.getId()).username(user.getUsername())
                .displayName(displayNameResolver.resolvePublic(user)).avatarUrl(user.getAvatarUrl())
                .title(post.getTitle()).content(post.getContent()).coverImage(post.getCoverImage())
                .images(parseImages(post.getImages())).likeCount(post.getLikeCount()).likedByMe(likedByMe)
                .replyCount(post.getReplyCount()).viewCount(post.getViewCount()).pinned(post.getPinned())
                .featured(post.getFeatured()).lastRepliedAt(post.getLastRepliedAt())
                .createdAt(post.getCreatedAt()).updatedAt(post.getUpdatedAt()).build();
    }

    public CommunityReplyDto toReplyDto(CommunityReply reply) {
        return toReplyDto(reply, false);
    }

    public CommunityReplyDto toReplyDto(CommunityReply reply, boolean likedByMe) {
        User user = reply.getUser();
        return CommunityReplyDto.builder()
                .id(reply.getId()).postId(reply.getPost().getId()).userId(user.getId())
                .username(user.getUsername()).displayName(displayNameResolver.resolvePublic(user))
                .avatarUrl(user.getAvatarUrl()).content(reply.getContent()).likeCount(reply.getLikeCount())
                .likedByMe(likedByMe)
                .createdAt(reply.getCreatedAt()).updatedAt(reply.getUpdatedAt()).build();
    }

    public String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (IOException exception) {
            throw new IllegalArgumentException("帖子图片数据格式错误", exception);
        }
    }

    private List<String> parseImages(String json) {
        if (!displayNameResolver.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException exception) {
            return List.of();
        }
    }

    private List<String> splitTags(String tags) {
        if (!displayNameResolver.hasText(tags)) return List.of();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(displayNameResolver::hasText).toList();
    }
}
