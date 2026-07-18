package com.aniglow.service;

import com.aniglow.dto.agent.AgentVoteCandidateDto;
import com.aniglow.dto.agent.AgentVoteStatusResponse;
import com.aniglow.entity.*;
import com.aniglow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentVoteService {

    private final AgentVoteRepository agentVoteRepository;
    private final AgentVoteCandidateRepository candidateRepository;
    private final AgentCharacterRepository agentCharacterRepository;

    public static String currentWeekKey() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int week = now.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        return String.format("%d-W%02d", year, week);
    }

    @Transactional
    public List<AgentVoteCandidateDto> getCandidates() {
        String weekKey = currentWeekKey();
        if (!candidateRepository.existsByWeekKey(weekKey)) {
            seedCandidatesForWeek(weekKey);
        }
        return candidateRepository.findByWeekKeyOrderByVoteCountDesc(weekKey)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void seedCandidatesForWeek(String weekKey) {
        // 默认候选池：五等分的新娘
        List<AgentVoteCandidate> defaults = List.of(
                AgentVoteCandidate.builder()
                        .code("Ichika").displayName("中野一花").sourceTitle("五等分的新娘")
                        .avatarUrl("/images/vote1.jpg")
                        .personality("成熟性感的大姐姐，五胞胎中的长女。表面大大咧咧，内心却细腻温柔，为了妹妹们可以牺牲自己。梦想成为演员。")
                        .speechStyle("慵懒随性的语气，略带小恶魔般的调侃，偶尔展露出大姐姐的温柔。自称\"私\"，爱用\"〜よ\"结尾。")
                        .catchphrases("あらあら〜、ふふっ、しょうがないな〜")
                        .weekKey(weekKey).voteCount(0L).isWinner(false).build(),
                AgentVoteCandidate.builder()
                        .code("Nino").displayName("中野二乃").sourceTitle("五等分的新娘")
                        .avatarUrl("/images/vote2.jpg")
                        .personality("傲娇强势的美少女，五胞胎中的次女。表面上毒舌又高冷，实际内心柔软，对家人极度重视。擅长料理和家务。")
                        .speechStyle("直率泼辣，说话不留情面，但面对真心时会变得害羞。多用命令形和强气语气，反差萌代表。")
                        .catchphrases("ばか！、べつに…、あんたなんか！")
                        .weekKey(weekKey).voteCount(0L).isWinner(false).build(),
                AgentVoteCandidate.builder()
                        .code("Miku").displayName("中野三玖").sourceTitle("五等分的新娘")
                        .avatarUrl("/images/vote3.jpg")
                        .personality("沉默寡言的三女，五胞胎中的历史爱好者。看似冷酷实则胆小怕生，内心渴望被认可。擅长模仿其他姐妹。")
                        .speechStyle("声音低沉寡言，不善表达，常低头说话，偶尔爆发出惊人发言。自称\"私\"，语气平稳安静。")
                        .catchphrases("好き…、私でいいの？、がんばる…")
                        .weekKey(weekKey).voteCount(0L).isWinner(false).build()
        );
        candidateRepository.saveAll(defaults);
    }

    @Transactional
    public AgentVoteStatusResponse vote(Long userId, Long candidateId) {
        String weekKey = currentWeekKey();

        Optional<AgentVote> existing = agentVoteRepository.findByUserIdAndWeekKey(userId, weekKey);
        if (existing.isPresent()) {
            return buildStatus(userId, weekKey);
        }

        AgentVoteCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("候选角色不存在"));

        AgentVote vote = AgentVote.builder()
                .user(User.builder().id(userId).build())
                .candidate(candidate)
                .weekKey(weekKey)
                .build();
        agentVoteRepository.save(vote);

        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateRepository.save(candidate);

        return buildStatus(userId, weekKey);
    }

    @Transactional(readOnly = true)
    public AgentVoteStatusResponse getStatus(Long userId) {
        return buildStatus(userId, currentWeekKey());
    }

    private AgentVoteStatusResponse buildStatus(Long userId, String weekKey) {
        Optional<AgentVote> existing = agentVoteRepository.findByUserIdAndWeekKey(userId, weekKey);

        List<AgentVoteCandidateDto> candidates = candidateRepository
                .findByWeekKeyOrderByVoteCountDesc(weekKey)
                .stream()
                .map(this::toDto)
                .toList();

        return AgentVoteStatusResponse.builder()
                .hasVoted(existing.isPresent())
                .votedCandidateId(existing.map(v -> v.getCandidate().getId()).orElse(null))
                .candidates(candidates)
                .build();
    }

    @Scheduled(cron = "0 0 0 * * THU")
    @Transactional
    public void determineWeeklyWinner() {
        String weekKey = currentWeekKey();

        List<AgentVoteCandidate> candidates = candidateRepository
                .findByWeekKeyOrderByVoteCountDesc(weekKey);

        if (candidates.isEmpty()) return;

        AgentVoteCandidate winner = candidates.get(0);
        if (winner.getVoteCount() <= 0) return;

        int maxSortOrder = agentCharacterRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()
                .stream()
                .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                .max()
                .orElse(0);

        AgentCharacter character = AgentCharacter.builder()
                .code(winner.getCode())
                .displayName(winner.getDisplayName())
                .sourceTitle(winner.getSourceTitle())
                .avatarUrl(winner.getAvatarUrl())
                .personality(winner.getPersonality())
                .speechStyle(winner.getSpeechStyle())
                .catchphrases(winner.getCatchphrases())
                .extraPrompt(winner.getExtraPrompt())
                .enabled(true)
                .sortOrder(maxSortOrder + 1)
                .build();
        agentCharacterRepository.save(character);

        winner.setIsWinner(true);
        candidateRepository.save(winner);
    }

    private AgentVoteCandidateDto toDto(AgentVoteCandidate c) {
        return AgentVoteCandidateDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .displayName(c.getDisplayName())
                .sourceTitle(c.getSourceTitle())
                .avatarUrl(c.getAvatarUrl())
                .voteCount(c.getVoteCount())
                .weekKey(c.getWeekKey())
                .build();
    }
}
