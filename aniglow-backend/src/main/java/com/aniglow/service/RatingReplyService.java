package com.aniglow.service;

import com.aniglow.dto.rating.RatingReplyDto;
import com.aniglow.dto.rating.RatingReplyRequest;
import com.aniglow.entity.Rating;
import com.aniglow.entity.RatingReply;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.mapper.RatingMapper;
import com.aniglow.repository.RatingReplyRepository;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingReplyService {

    private final RatingRepository ratingRepository;
    private final RatingReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final RatingMapper ratingMapper;

    @Transactional(readOnly = true)
    public List<RatingReplyDto> list(Long ratingId, int page, int size) {
        validatePage(page, size);
        return replyRepository.findByRatingIdOrderByCreatedAtAsc(ratingId, PageRequest.of(page, size))
                .getContent().stream().map(ratingMapper::toReplyDto).toList();
    }

    @Transactional
    public RatingReplyDto create(Long ratingId, Long userId, RatingReplyRequest request) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("评分评论", "id", ratingId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
        userProfileService.syncPublicProfile(user, request.getDisplayName(), request.getAvatarUrl());
        RatingReply reply = RatingReply.builder()
                .rating(rating).user(user).content(request.getContent().trim()).likeCount(0L).build();
        return ratingMapper.toReplyDto(replyRepository.save(reply));
    }

    @Transactional
    public RatingReplyDto like(Long replyId) {
        RatingReply reply = find(replyId);
        reply.setLikeCount((reply.getLikeCount() == null ? 0L : reply.getLikeCount()) + 1);
        return ratingMapper.toReplyDto(replyRepository.save(reply));
    }

    @Transactional
    public void delete(Long replyId, Long userId) {
        RatingReply reply = find(replyId);
        if (!reply.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("无权删除他人的回复");
        }
        replyRepository.delete(reply);
    }

    private RatingReply find(Long id) {
        return replyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论回复", "id", id));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("分页参数无效");
    }
}
