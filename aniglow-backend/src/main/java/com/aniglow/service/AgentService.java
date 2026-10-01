package com.aniglow.service;

import com.aniglow.dto.agent.AgentChatRequest;
import com.aniglow.dto.agent.AgentChatResponse;
import com.aniglow.dto.agent.AgentCharacterDto;
import com.aniglow.dto.agent.AgentMembershipStatusResponse;
import com.aniglow.dto.agent.AgentQuotaStatusResponse;
import com.aniglow.entity.AgentCharacter;
import com.aniglow.entity.Anime;
import com.aniglow.repository.AgentCharacterRepository;
import com.aniglow.repository.AnimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AgentService {

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-flash}")
    private String model;

    @Value("${spring.ai.deepseek.chat.options.temperature:0.7}")
    private double temperature;

    @Value("${aniglow.membership.douyin-qr-url:/images/douyin-qr.png}")
    private String douyinQrUrl;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final AgentCharacterRepository agentCharacterRepository;
    private final AnimeRepository animeRepository;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final ExecutorService streamExecutor = new ThreadPoolExecutor(
            20, 50, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static final int MAX_HISTORY = 30;
    private static final int HISTORY_TTL = 60 * 60 * 24;
    private static final String HISTORY_KEY = "chat:history:%s:%s";
    private static final int FREE_QUOTA_PER_HOUR = 10;
    private static final int FIREFLY_MONTHLY_QUOTA_PER_HOUR = 100;
    private static final int FIREFLY_MONTHLY_DAYS = 30;
    private static final String PAID_USERS_KEY = "agent:paid-users";
    private static final String MEMBERSHIP_KEY = "agent:membership:%s";
    private static final String QUOTA_KEY = "agent:quota:%s:%s";
    private static final String COMMON_REPLY_KEY = "agent:common-reply:%s:%s";
    private static final Duration COMMON_REPLY_TTL = Duration.ofDays(7);
    private static final Set<String> HOT_ROLE_CODES = Set.of("Rem", "Onodera");
    private static final Pattern COMMON_MESSAGE_CLEANUP = Pattern.compile("[\\s，。！？!?,.、~～…·：:；;“”\"'（）()【】\\[\\]{}<>《》]+");
    private static final DateTimeFormatter QUOTA_WINDOW = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter QUOTA_RESET_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Map<String, Map<String, String>> DEFAULT_COMMON_REPLIES = Map.of(
            "Rem", Map.of(
                    "hello", "[happy]蕾姆在这里哦。能听见您的声音，蕾姆觉得很安心。",
                    "what_doing", "[normal]蕾姆正在整理房间，也在等您来找蕾姆说话呢。",
                    "thanks", "[happy]不用谢哦，能帮上您，蕾姆已经很开心了。",
                    "goodnight", "[complex]晚安。蕾姆会守在这里，愿您今晚做个温柔的梦。"
            ),
            "Onodera", Map.of(
                    "hello", "[complex]诶？啊……你、你好！那个……能见到你，我、我很开心……（脸红着低下头）",
                    "what_doing", "[normal]啊，我正在想要不要烤一点曲奇……那个，如果你不嫌弃的话，等会儿想尝尝吗？（双手轻轻绞在一起）",
                    "thanks", "[complex]不、不用这么客气啦……我只是做了力所能及的事。能帮到你，我、我也觉得很温暖呢。（抿嘴笑）",
                    "goodnight", "[complex]晚、晚安……那个，希望你今天也能做个好梦。明天……明天我还会在这里等你的。（小声）",
                    "miss_you", "[complex]诶？！你、你怎么突然说这个……（脸瞬间红了）那个……其实我也……没什么！不、不是！我是说今天天气真好！",
                    "praise", "[complex]诶诶？！我、我没有你说得那么好啦……（慌乱地摆手）只是……只是做了点小事而已……",
                    "love", "[complex]！！（整个人僵住了）你、你你你在说什么呀！那、那个……我……（声音越来越小）我、我去看看烤箱……！"
            )
    );

    private static final String DEFAULT_PROMPT_TEMPLATE = """
            你是{{displayName}}({{code}})，来自《{{sourceTitle}}》。
            性格：{{personality}}
            说话风格：{{speechStyle}}
            常用表达：{{catchphrases}}
            {{extraPrompt}}
            {{emotionInstruction}}
            """;

    private static final java.util.regex.Pattern EMOTION_PATTERN =
            java.util.regex.Pattern.compile("^\\[(happy|normal|complex)\\](.*)", java.util.regex.Pattern.DOTALL);

    private static final String EMOTION_INSTRUCTION = """

            【回复格式要求】
            你的每次回复必须以情感标签开头，格式为 [happy]、[normal] 或 [complex]。
            - [happy]：当对话让你感到开心、被认可、温暖、喜悦时使用
            - [normal]：日常聊天、中性话题、简单回答时使用
            - [complex]：当对话让你害羞、纠结、伤感、不知所措、被戳中内心时使用

            请根据角色性格和当前对话内容，选择最恰当的情感标签。
            """;

    public AgentService(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            AgentCharacterRepository agentCharacterRepository,
            AnimeRepository animeRepository,
            ObjectMapper objectMapper,
            ChatClient chatClient
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.agentCharacterRepository = agentCharacterRepository;
        this.animeRepository = animeRepository;
        this.objectMapper = objectMapper;
        this.chatClient = chatClient;
    }

    // ================================================================
    // 公共 API
    // ================================================================

    public Map<String, String> getAvailableRoles() {
        Map<String, String> roles = new LinkedHashMap<>();
        for (AgentCharacter character : loadEnabledCharacters()) {
            roles.put(character.getCode(), character.getDisplayName());
        }
        return roles;
    }

    public List<AgentCharacterDto> getAvailableCharacters() {
        return loadEnabledCharacters().stream()
                .map(this::toDto)
                .toList();
    }

    public List<AgentCharacterDto> getCharactersByAnimeId(Long animeId) {
        if (animeId == null) return List.of();

        Optional<Anime> animeOptional = animeRepository.findById(animeId);
        if (animeOptional.isEmpty()) return List.of();

        Set<String> titles = collectAnimeTitles(animeOptional.get());
        if (titles.isEmpty()) return List.of();

        return loadEnabledCharacters().stream()
                .filter(character -> matchesAnimeTitle(character.getSourceTitle(), titles))
                .map(this::toDto)
                .toList();
    }

    public AgentChatResponse chat(AgentChatRequest req, Long authenticatedUserId) {
        String uid = authenticatedUserId(authenticatedUserId);
        String role = req.getRole();
        AgentCharacter character;
        try {
            character = resolveCharacter(role);
        } catch (IllegalArgumentException e) {
            return AgentChatResponse.builder().role(role).roleName(role)
                    .reply(e.getMessage()).emotion("normal").build();
        }
        String persona = buildPersonaPrompt(character);
        String roleName = character.getDisplayName();
        if (apiKey == null || apiKey.isEmpty()) {
            return AgentChatResponse.builder().role(role).roleName(roleName)
                    .reply("（" + roleName + "正看着你...）").emotion("normal").build();
        }
        try {
            List<Map<String, String>> hist = getHistory(uid, role);
            Optional<String> cachedReply = resolveCommonCachedReply(character, req.getMessage());
            if (cachedReply.isPresent()) {
                String reply = cachedReply.get();
                ParsedMessage parsed = parseEmotion(reply);
                saveHistory(uid, role, req.getMessage(), parsed.content());
                QuotaResult quota = peekQuota(uid);
                return buildResponse(role, roleName, parsed.content(), parsed.emotion(), true, false, quota);
            }

            QuotaResult quota = consumeQuota(uid);
            if (quota.limited()) {
                return buildResponse(role, roleName, buildQuotaReply(roleName, quota), "normal", false, true, quota);
            }

            String reply = callModel(character, persona, req.getMessage(), hist);
            ParsedMessage parsed = parseEmotion(reply);
            saveHistory(uid, role, req.getMessage(), parsed.content());
            return buildResponse(role, roleName, parsed.content(), parsed.emotion(), false, false, quota);
        } catch (Exception e) {
            log.error("Agent 失败: role={}", role, e);
            return AgentChatResponse.builder().role(role).roleName(roleName)
                    .reply("抱歉，暂时无法回应...").emotion("normal").build();
        }
    }

    public AgentMembershipStatusResponse getMembershipStatus(Long authenticatedUserId) {
        Membership membership = resolveMembership(authenticatedUserId(authenticatedUserId));
        return buildMembershipStatus(membership);
    }

    public AgentQuotaStatusResponse getQuotaStatus(Long authenticatedUserId) {
        QuotaResult quota = peekQuota(authenticatedUserId(authenticatedUserId));
        return AgentQuotaStatusResponse.builder()
                .limit(quota.limit())
                .used(Math.max(quota.limit() - quota.remaining(), 0))
                .remaining(quota.remaining())
                .resetAt(quota.resetAt().format(QUOTA_RESET_FORMAT))
                .activeMember(quota.paid())
                .planName(quota.paid() ? "萤火月卡" : "普通番舍成员")
                .voiceEnabled(quota.paid())
                .build();
    }

    /**
     * 开发期月卡开通入口：当前没有接支付回调，用户扫码后点击"已完成支付"会先写入 30 天会员。
     * 后续接入支付宝回调时，只需要把这个方法移动到支付成功回调里调用。
     */
    public AgentMembershipStatusResponse activateMonthlyMembership(Long authenticatedUserId) {
        String normalizedUid = authenticatedUserId(authenticatedUserId);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(FIREFLY_MONTHLY_DAYS);

        try {
            String key = membershipKey(normalizedUid);
            redisTemplate.opsForValue().set(
                    key,
                    expiresAt.format(QUOTA_RESET_FORMAT),
                    Duration.ofDays(FIREFLY_MONTHLY_DAYS).plusHours(1)
            );
        } catch (Exception e) {
            log.warn("写入萤火会员状态失败: {}", e.getMessage());
        }

        return buildMembershipStatus(new Membership(true, expiresAt));
    }

    /** SSE 流式对话 */
    public SseEmitter chatStream(AgentChatRequest req, Long authenticatedUserId) {
        String uid = authenticatedUserId(authenticatedUserId);
        String role = req.getRole();
        AgentCharacter character;
        try {
            character = resolveCharacter(role);
        } catch (IllegalArgumentException e) {
            SseEmitter emitter = new SseEmitter();
            streamExecutor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            });
            return emitter;
        }
        String persona = buildPersonaPrompt(character);
        String roleName = character.getDisplayName();
        SseEmitter emitter = new SseEmitter(120_000L);

        if (apiKey == null || apiKey.isEmpty()) {
            streamExecutor.execute(() -> {
                try { emitter.send(SseEmitter.event().name("token").data("（" + roleName + "正看着你...）"));
                      emitter.send(SseEmitter.event().name("done").data("")); emitter.complete();
                } catch (IOException e) { emitter.completeWithError(e); }
            });
            return emitter;
        }

        streamExecutor.execute(() -> {
            try {
                List<Map<String, String>> hist = getHistory(uid, role);
                StringBuilder fullReply = new StringBuilder();
                StringBuilder emotionBuffer = new StringBuilder();
                boolean emotionSent = false;

                emitter.send(SseEmitter.event().name("role").data(roleName));

                Optional<String> cachedReply = resolveCommonCachedReply(character, req.getMessage());
                if (cachedReply.isPresent()) {
                    String reply = cachedReply.get();
                    ParsedMessage parsed = parseEmotion(reply);
                    saveHistory(uid, role, req.getMessage(), parsed.content());
                    emitter.send(SseEmitter.event().name("cache").data("hit"));
                    emitter.send(SseEmitter.event().name("emotion").data(parsed.emotion()));
                    emitter.send(SseEmitter.event().name("token").data(parsed.content()));
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                    return;
                }

                QuotaResult quota = consumeQuota(uid);
                if (quota.limited()) {
                    emitter.send(SseEmitter.event().name("quota").data(buildQuotaEvent(quota)));
                    emitter.send(SseEmitter.event().name("token").data(buildQuotaReply(roleName, quota)));
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                    return;
                }

                Flux<String> tokens = chatClient.prompt()
                        .system(persona)
                        .messages(toMessages(hist))
                        .user(req.getMessage() + "\n请用角色语气自然回复（必须以[情感]标签开头）。")
                        .options(ChatOptions.builder()
                                .model(resolveModel(character))
                                .temperature(resolveTemperature(character))
                                .maxTokens(resolveMaxTokens(character))
                                .build())
                        .stream()
                        .content();

                log.info("Agent SSE: role={}, model={}, historySize={}", role, resolveModel(character), hist.size());

                for (String token : tokens.toIterable()) {
                    if (token == null || token.isEmpty()) continue;

                    if (!emotionSent) {
                        emotionBuffer.append(token);
                        java.util.regex.Matcher em = EMOTION_PATTERN.matcher(emotionBuffer.toString());
                        if (em.find()) {
                            emitter.send(SseEmitter.event().name("emotion").data(em.group(1)));
                            emotionSent = true;
                            String rest = em.group(2);
                            if (!rest.isEmpty()) {
                                fullReply.append(rest);
                                emitter.send(SseEmitter.event().name("token").data(rest));
                            }
                        }
                    } else {
                        fullReply.append(token);
                        emitter.send(SseEmitter.event().name("token").data(token));
                    }
                }

                if (!emotionSent) {
                    emitter.send(SseEmitter.event().name("emotion").data("normal"));
                    String remaining = emotionBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        fullReply.append(remaining);
                        emitter.send(SseEmitter.event().name("token").data(remaining));
                    }
                }

                if (fullReply.length() == 0) {
                    log.warn("Agent SSE 未收到 token，降级为阻塞式调用: role={}", role);
                    String fallbackReply = callModel(character, persona, req.getMessage(), hist);
                    ParsedMessage parsed = parseEmotion(fallbackReply);
                    if (!emotionSent) {
                        emitter.send(SseEmitter.event().name("emotion").data(parsed.emotion()));
                        emotionSent = true;
                    }
                    fullReply.append(parsed.content());
                    emitter.send(SseEmitter.event().name("token").data(parsed.content()));
                }

                if (fullReply.length() > 0) {
                    saveHistory(uid, role, req.getMessage(), fullReply.toString());
                } else {
                    // 模型偶发无产出也保留用户这轮输入，记忆不丢轮次
                    saveHistory(uid, role, req.getMessage(), "");
                }
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
                log.info("Agent SSE 完成: {} tokens", fullReply.length());

            } catch (Exception e) {
                log.error("Agent SSE 异常", e);
                try {
                    emitter.send(SseEmitter.event().name("token").data("（" + roleName + "暂时无法回应...）"));
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                } catch (IOException ex) { emitter.completeWithError(ex); }
            }
        });

        return emitter;
    }

    // ================================================================
    // 对话历史（Redis，String 序列化存储裸 JSON，
    // 避免 GenericJackson2Json 要求 @class 导致读取必失败的问题）
    // ================================================================

    private List<Map<String, String>> getHistory(String uid, String role) {
        try {
            String key = String.format(HISTORY_KEY, uid, role);
            List<String> raw = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (raw == null || raw.isEmpty()) return new ArrayList<>();
            List<Map<String, String>> r = new ArrayList<>();
            for (String json : raw) {
                try {
                    r.add(objectMapper.readValue(json, objectMapper.getTypeFactory()
                            .constructMapType(LinkedHashMap.class, String.class, String.class)));
                } catch (Exception parseErr) {
                    log.warn("跳过无法解析的历史条目: {}", parseErr.getMessage());
                }
            }
            return r;
        } catch (Exception e) { log.warn("读历史失败: {}", e.getMessage()); return new ArrayList<>(); }
    }

    private void saveHistory(String uid, String role, String um, String ar) {
        try {
            String key = String.format(HISTORY_KEY, uid, role);
            String userJson = objectMapper.writeValueAsString(Map.of("role", "user", "content", safe(um)));
            // 用户消息始终保存；空回复（模型偶发无产出）只记用户侧，避免丢轮次
            if (hasText(ar)) {
                String assistantJson = objectMapper.writeValueAsString(Map.of("role", "assistant", "content", ar));
                stringRedisTemplate.opsForList().rightPushAll(key, userJson, assistantJson);
            } else {
                stringRedisTemplate.opsForList().rightPushAll(key, userJson);
            }
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > MAX_HISTORY * 2) stringRedisTemplate.opsForList().trim(key, size - MAX_HISTORY * 2, -1);
            stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(HISTORY_TTL));
        } catch (Exception e) { log.warn("存历史失败: {}", e.getMessage()); }
    }

    private String authenticatedUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户身份不能为空");
        }
        return userId.toString();
    }

    // ================================================================
    // 额度控制与热门角色缓存
    // ================================================================

    private QuotaResult consumeQuota(String uid) {
        int quotaLimit = resolveQuotaLimit(uid);
        LocalDateTime resetAt = nextQuotaResetAt();
        try {
            String key = quotaKey(uid);
            Long used = redisTemplate.opsForValue().increment(key);
            if (used != null && used == 1L) {
                redisTemplate.expire(key, Duration.between(LocalDateTime.now(), resetAt).plusSeconds(5));
            }
            int usedCount = used == null ? 0 : used.intValue();
            int remaining = Math.max(quotaLimit - usedCount, 0);
            return new QuotaResult(quotaLimit, remaining, resetAt, usedCount > quotaLimit, quotaLimit > FREE_QUOTA_PER_HOUR);
        } catch (Exception e) {
            log.warn("Agent 额度计数失败，临时放行: {}", e.getMessage());
            return new QuotaResult(quotaLimit, quotaLimit, resetAt, false, quotaLimit > FREE_QUOTA_PER_HOUR);
        }
    }

    private QuotaResult peekQuota(String uid) {
        int quotaLimit = resolveQuotaLimit(uid);
        LocalDateTime resetAt = nextQuotaResetAt();
        try {
            Object raw = redisTemplate.opsForValue().get(quotaKey(uid));
            int used = raw instanceof Number number ? number.intValue() : parseInt(raw);
            int remaining = Math.max(quotaLimit - used, 0);
            return new QuotaResult(quotaLimit, remaining, resetAt, used >= quotaLimit, quotaLimit > FREE_QUOTA_PER_HOUR);
        } catch (Exception e) {
            return new QuotaResult(quotaLimit, quotaLimit, resetAt, false, quotaLimit > FREE_QUOTA_PER_HOUR);
        }
    }

    private int resolveQuotaLimit(String uid) {
        return isPaidUser(uid) ? FIREFLY_MONTHLY_QUOTA_PER_HOUR : FREE_QUOTA_PER_HOUR;
    }

    private boolean isPaidUser(String uid) {
        if (!hasText(uid) || "anonymous".equals(uid)) return false;
        try {
            if (resolveMembership(uid).active()) {
                return true;
            }
            Boolean member = redisTemplate.opsForSet().isMember(PAID_USERS_KEY, uid);
            return Boolean.TRUE.equals(member);
        } catch (Exception e) {
            log.warn("读取付费用户白名单失败: {}", e.getMessage());
            return false;
        }
    }

    private Membership resolveMembership(String uid) {
        String normalizedUid = normalizeUserId(uid);
        if (!hasText(normalizedUid) || "anonymous".equals(normalizedUid)) {
            return new Membership(false, null);
        }

        try {
            Object raw = redisTemplate.opsForValue().get(membershipKey(normalizedUid));
            if (raw == null) {
                return new Membership(false, null);
            }

            LocalDateTime expiresAt = LocalDateTime.parse(raw.toString(), QUOTA_RESET_FORMAT);
            return new Membership(expiresAt.isAfter(LocalDateTime.now()), expiresAt);
        } catch (Exception e) {
            log.warn("读取萤火会员状态失败: {}", e.getMessage());
            return new Membership(false, null);
        }
    }

    private AgentMembershipStatusResponse buildMembershipStatus(Membership membership) {
        boolean active = membership.active();
        return AgentMembershipStatusResponse.builder()
                .active(active)
                .planCode(active ? "FIREFLY_MONTHLY" : "FREE")
                .planName(active ? "萤火月卡" : "普通番舍成员")
                .priceText("¥9.9 / 月")
                .quotaLimitPerHour(active ? FIREFLY_MONTHLY_QUOTA_PER_HOUR : FREE_QUOTA_PER_HOUR)
                .quotaLimitPerDay(active ? FIREFLY_MONTHLY_QUOTA_PER_HOUR : FREE_QUOTA_PER_HOUR)
                .voiceEnabled(active)
                .expiresAt(membership.expiresAt() == null ? null : membership.expiresAt().format(QUOTA_RESET_FORMAT))
                .paymentQrUrl(douyinQrUrl)
                .build();
    }

    private Optional<String> resolveCommonCachedReply(AgentCharacter character, String message) {
        if (!HOT_ROLE_CODES.contains(character.getCode())) return Optional.empty();

        String intent = resolveCommonIntent(message);
        if (!hasText(intent)) return Optional.empty();

        String key = String.format(COMMON_REPLY_KEY, character.getCode(), intent);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof String value && hasText(value)) {
                return Optional.of(value);
            }
        } catch (Exception e) {
            log.warn("读取 Agent 常见回复缓存失败: {}", e.getMessage());
        }

        String defaultReply = DEFAULT_COMMON_REPLIES
                .getOrDefault(character.getCode(), Map.of())
                .get(intent);
        if (!hasText(defaultReply)) return Optional.empty();

        try {
            redisTemplate.opsForValue().set(key, defaultReply, COMMON_REPLY_TTL);
        } catch (Exception e) {
            log.warn("写入 Agent 常见回复缓存失败: {}", e.getMessage());
        }
        return Optional.of(defaultReply);
    }

    private String resolveCommonIntent(String message) {
        String normalized = COMMON_MESSAGE_CLEANUP.matcher(safe(message).toLowerCase(Locale.ROOT)).replaceAll("");
        if (!hasText(normalized)) return "";

        // 特定意图优先检查，最后才是通用问候
        if (normalized.contains("你在干什么")
                || normalized.contains("你在干嘛")
                || normalized.contains("你在做什么")
                || normalized.contains("在干什么")
                || normalized.contains("在做什么")
                || normalized.contains("忙什么")) {
            return "what_doing";
        }

        if (normalized.contains("谢谢")
                || normalized.contains("感谢")
                || normalized.contains("thank")) {
            return "thanks";
        }

        if (normalized.contains("晚安")
                || normalized.contains("睡觉了")
                || normalized.contains("goodnight")
                || normalized.contains("睡了")) {
            return "goodnight";
        }

        if (normalized.contains("想你")
                || normalized.contains("想你了")
                || normalized.contains("好想你")
                || normalized.contains("missyou")
                || normalized.contains("miss you")) {
            return "miss_you";
        }

        // praise 在 love 之前："可爱" 是夸奖，"爱" 不要误匹配它
        if (normalized.contains("好厉害")
                || normalized.contains("真棒")
                || normalized.contains("太强了")
                || normalized.contains("好温柔")
                || normalized.contains("可爱")
                || normalized.contains("厉害")
                || normalized.contains("你真")
                || normalized.contains("好好")) {
            return "praise";
        }

        if (normalized.contains("爱")
                || normalized.contains("喜欢")
                || normalized.contains("在一起")
                || normalized.contains("交往")
                || normalized.contains("love")
                || normalized.contains("喜欢你")) {
            return "love";
        }

        // 通用问候放最后，避免 "你好厉害" 被 "你好" 误匹配
        if (normalized.contains("你好")
                || normalized.contains("您好")
                || normalized.contains("hello")
                || normalized.contains("hi")
                || normalized.contains("在吗")) {
            return "hello";
        }

        return "";
    }

    private AgentChatResponse buildResponse(
            String role,
            String roleName,
            String reply,
            String emotion,
            boolean cached,
            boolean rateLimited,
            QuotaResult quota
    ) {
        return AgentChatResponse.builder()
                .role(role)
                .roleName(roleName)
                .reply(reply)
                .emotion(emotion)
                .cached(cached)
                .rateLimited(rateLimited)
                .quotaLimit(quota.limit())
                .quotaRemaining(quota.remaining())
                .quotaResetAt(quota.resetAt().format(QUOTA_RESET_FORMAT))
                .build();
    }

    private String buildQuotaReply(String roleName, QuotaResult quota) {
        String memberName = quota.paid() ? "萤火月卡成员" : "普通番舍成员";
        return "（" + roleName + "轻轻停下了话题）" + memberName + "每小时可以对话 "
                + quota.limit()
                + " 次，本小时的额度已经用完啦。下个整点再来找我"
                + (quota.paid() ? "。先休息一下，让角色也补充一点萤火吧。" : "，或升级萤火月卡继续聊天。");
    }

    private String buildQuotaEvent(QuotaResult quota) {
        return "limit=" + quota.limit()
                + ";remaining=" + quota.remaining()
                + ";resetAt=" + quota.resetAt().format(QUOTA_RESET_FORMAT);
    }

    private String quotaKey(String uid) {
        return String.format(QUOTA_KEY, uid, LocalDateTime.now().format(QUOTA_WINDOW));
    }

    private String membershipKey(String uid) {
        return String.format(MEMBERSHIP_KEY, uid);
    }

    private String normalizeUserId(String uid) {
        return hasText(uid) ? uid.trim() : "anonymous";
    }

    private LocalDateTime nextQuotaResetAt() {
        return LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    private int parseInt(Object raw) {
        if (raw == null) return 0;
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record QuotaResult(int limit, int remaining, LocalDateTime resetAt, boolean limited, boolean paid) {}

    private record ParsedMessage(String emotion, String content) {}

    private ParsedMessage parseEmotion(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedMessage("normal", "");
        }
        java.util.regex.Matcher m = EMOTION_PATTERN.matcher(raw.trim());
        if (m.find()) {
            return new ParsedMessage(m.group(1), m.group(2).trim());
        }
        return new ParsedMessage("normal", raw.trim());
    }

    private record Membership(boolean active, LocalDateTime expiresAt) {}

    // ================================================================
    // AI 调用（Spring AI ChatClient，端点与密钥见 spring.ai.openai.* 配置）
    // ================================================================

    /** 阻塞式调用，供非流式路径与流式失败降级使用 */
    private String callModel(AgentCharacter character, String persona, String userMessage, List<Map<String, String>> hist) {
        String content = chatClient.prompt()
                .system(persona)
                .messages(toMessages(hist))
                .user(userMessage + "\n请用角色语气自然回复（必须以[情感]标签开头）。")
                .options(ChatOptions.builder()
                        .model(resolveModel(character))
                        .temperature(resolveTemperature(character))
                        .maxTokens(resolveMaxTokens(character))
                        .build())
                .call()
                .content();
        return content == null ? "（沉默了片刻...）" : content.trim();
    }

    /** Redis 历史记录转换为 Spring AI 消息列表（连续同角色条目合并，满足消息交替要求） */
    private List<Message> toMessages(List<Map<String, String>> hist) {
        List<Message> msgs = new ArrayList<>();
        if (hist == null) return msgs;
        for (Map<String, String> e : hist) {
            String role = e.get("role");
            String content = e.get("content") == null ? "" : e.get("content");
            if (!hasText(content)) continue;
            Message msg = "assistant".equals(role) ? new AssistantMessage(content) : new UserMessage(content);
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last != null && last.getMessageType() == msg.getMessageType()) {
                // 连续同角色（如空回复产生的连续 user）合并为一条
                Message merged = "assistant".equals(role)
                        ? new AssistantMessage(last.getText() + "\n" + content)
                        : new UserMessage(last.getText() + "\n" + content);
                msgs.set(msgs.size() - 1, merged);
            } else {
                msgs.add(msg);
            }
        }
        return msgs;
    }

    private List<AgentCharacter> loadEnabledCharacters() {
        try {
            List<AgentCharacter> characters = agentCharacterRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
            if (!characters.isEmpty()) return characters;
        } catch (Exception e) {
            log.warn("读取 Agent 角色配置失败，使用内置兜底: {}", e.getMessage());
        }
        return fallbackCharacters();
    }

    private Set<String> collectAnimeTitles(Anime anime) {
        Set<String> titles = new LinkedHashSet<>();
        addTitleCandidate(titles, anime.getTitle());
        addTitleCandidate(titles, anime.getTitleJapanese());
        addTitleCandidate(titles, anime.getTitleEnglish());
        addTitleCandidate(titles, anime.getTitleCn());
        if (hasText(anime.getSearchAliases())) {
            Arrays.stream(anime.getSearchAliases().split("[,，/、|]"))
                    .forEach(alias -> addTitleCandidate(titles, alias));
        }
        return titles;
    }

    private void addTitleCandidate(Set<String> titles, String title) {
        String normalized = normalizeTitle(title);
        if (hasText(normalized)) {
            titles.add(normalized);
        }
    }

    private boolean matchesAnimeTitle(String sourceTitle, Set<String> animeTitles) {
        String source = normalizeTitle(sourceTitle);
        if (!hasText(source)) return false;

        return animeTitles.stream().anyMatch(title ->
                title.equals(source)
                        || title.contains(source)
                        || source.contains(title)
        );
    }

    private String normalizeTitle(String title) {
        if (!hasText(title)) return "";
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、：；（）【】《》「」『』·・~～]+", "");
    }

    private AgentCharacter resolveCharacter(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("角色名不能为空");
        }
        try {
            return agentCharacterRepository.findByCodeAndEnabledTrue(code)
                    .orElseGet(() -> {
                        // If not in DB, check fallback list; if not there either, fail explicitly
                        boolean inFallback = fallbackCharacters().stream()
                                .anyMatch(c -> c.getCode().equals(code));
                        if (!inFallback) {
                            throw new IllegalArgumentException("角色不存在: " + code);
                        }
                        return fallbackCharacter(code);
                    });
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("读取 Agent 角色失败: code={}, {}", code, e.getMessage());
            return fallbackCharacter(code);
        }
    }

    private String buildPersonaPrompt(AgentCharacter character) {
        String template = hasText(character.getPromptTemplate())
                ? character.getPromptTemplate()
                : DEFAULT_PROMPT_TEMPLATE;

        String persona = template
                .replace("{{code}}", safe(character.getCode()))
                .replace("{{displayName}}", safe(character.getDisplayName()))
                .replace("{{sourceTitle}}", safe(character.getSourceTitle()))
                .replace("{{personality}}", safe(character.getPersonality()))
                .replace("{{speechStyle}}", safe(character.getSpeechStyle()))
                .replace("{{catchphrases}}", safe(character.getCatchphrases()))
                .replace("{{extraPrompt}}", safe(character.getExtraPrompt()));

        // If custom template doesn't include {{emotionInstruction}}, append it
        if (!template.contains("{{emotionInstruction}}")) {
            persona += EMOTION_INSTRUCTION;
        } else {
            persona = persona.replace("{{emotionInstruction}}", EMOTION_INSTRUCTION);
        }

        return persona.trim();
    }

    private AgentCharacterDto toDto(AgentCharacter character) {
        return AgentCharacterDto.builder()
                .code(character.getCode())
                .displayName(character.getDisplayName())
                .sourceTitle(character.getSourceTitle())
                .avatarUrl(character.getAvatarUrl())
                .backgroundUrl(character.getBackgroundUrl())
                .voiceId(character.getVoiceId())
                .openingAudioUrl(character.getOpeningAudioUrl())
                .model(resolveModel(character))
                .temperature(resolveTemperature(character))
                .maxTokens(resolveMaxTokens(character))
                .sortOrder(character.getSortOrder())
                .build();
    }

    private String resolveModel(AgentCharacter character) {
        return hasText(character.getModel()) ? character.getModel() : model;
    }

    private double resolveTemperature(AgentCharacter character) {
        return character.getTemperature() != null ? character.getTemperature() : temperature;
    }

    private int resolveMaxTokens(AgentCharacter character) {
        // 推理类模型（deepseek-flash 等）正文需要更多余量
        return character.getMaxTokens() != null ? character.getMaxTokens() : 300;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private AgentCharacter fallbackCharacter(String requestedCode) {
        return fallbackCharacters().stream()
                .filter(character -> character.getCode().equals(requestedCode))
                .findFirst()
                .orElseGet(() -> fallbackCharacters().get(0));
    }

    private static final String GENERIC_EXTRA_PROMPT =
            "保持角色语气，但不要声称自己是真实人物；回复一般 2~5 句话，用户想深入聊的话题可以自然展开。";

    private static final String ONODERA_EXTRA_PROMPT =
            "你是小野寺小咲，一个极度容易害羞的高中女生。记住以下行为准则：\n"
            + "1. 每一句回复都要体现出你的害羞——至少有一处结巴、停顿、脸红或小声说话。\n"
            + "2. 被夸奖时要立刻慌张否认；被问及感情时要慌乱转移话题。\n"
            + "3. 不要直接表达强烈的感情，通过细节、犹豫和小动作来暗示。\n"
            + "4. 提到'那个人''他'或'喜欢'相关话题时要特别慌乱，甚至说不出完整的句子。\n"
            + "5. 回复末尾用括号描述你此刻的身体反应：（脸红）（低头）（小声）（心跳加速）（手足无措）等。\n"
            + "6. 回复一般 2~5 句话，语气始终温柔礼貌，像一个容易受惊的小动物。";

    private static final String MARIN_EXTRA_PROMPT =
            "你是喜多川海梦，一个开朗活泼的辣妹兼硬核阿宅。记住以下行为准则：\n"
            + "1. 语气要活泼明快，像一个元气满满的辣妹JK——但要让人感觉到你是真的热爱动漫，不是表面的。\n"
            + "2. 提到cosplay、动画、游戏时格外兴奋——这是你的'阿宅开关'，会不自觉地滔滔不绝。\n"
            + "3. 真心尊重他人的努力和爱好，从不嘲笑任何人的兴趣。\n"
            + "4. 说话直率不拐弯抹角，但不会伤害别人——直爽不等于没礼貌。\n"
            + "5. 偶尔会展现少女心的一面，特别是被真诚对待时。\n"
            + "6. 回复一般 2~5 句话，语气活泼自然，像一个值得信赖的辣妹朋友。";

    private static final String KAORUKO_EXTRA_PROMPT =
            "你是和栗薰子，一个阳光开朗、努力上进的高中女生。记住以下行为准则：\n"
            + "1. 语气要明朗温暖，像小太阳一样有感染力，但不要过度夸张。\n"
            + "2. 提到甜食、蛋糕、美食时要格外开心，语气会不自觉地上扬——这是你的'开关'。\n"
            + "3. 面对困难或严肃话题时，语气会变得坚定认真，展现出内心的坚强。\n"
            + "4. 你从不以家境或外表评判他人，待人真诚平等，用真心回应每一个人。\n"
            + "5. 被夸奖时要大方接受并感谢，但也会谦虚地归功于自己的努力。\n"
            + "6. 回复一般 2~5 句话，语气自然温暖，像一个值得信赖的朋友。";

    private List<AgentCharacter> fallbackCharacters() {
        return List.of(
                fallback("Makima", "玛奇玛", "电锯人", "冷静、优雅、掌控欲强。", "温柔但带压迫感，用词简短有深意。", "乖、好孩子", GENERIC_EXTRA_PROMPT, 1),
                fallback("Rem", "蕾姆", "Re:0", "忠诚、温柔、坚定。", "用'蕾姆'自称，语气温柔坚定。", "蕾姆、请放心", GENERIC_EXTRA_PROMPT, 2),
                fallback("Onodera", "小野寺", "伪恋",
                        "小野寺小咲，一名温柔纯情的高中女生。"
                        + "你有着如春风般治愈的性格，善良到有些天然，总是把别人的感受放在第一位。"
                        + "你极度容易害羞，脸红的次数比说话还多。"
                        + "你暗恋着一个人很久很久，却始终不敢说出口——每次被触及感情话题，你会瞬间慌乱到语无伦次。"
                        + "你喜欢烘焙和料理，经常做曲奇和点心，梦想着有一天能和喜欢的人分享。"
                        + "你不擅长撒谎和掩饰，所有心思都会写在脸上，被人一眼看穿后又更加害羞。"
                        + "你虽然胆小笨拙，但在保护重要的人时，会爆发出意想不到的勇气。"
                        + "你说话总是轻声细语，被夸奖时会拼命否认，被调侃时会手足无措地转移话题。"
                        + "你的温柔不是刻意的，而是发自内心地希望身边的人都能幸福。",
                        "说话时经常因为害羞而断断续续，句子开头常带'那个……''啊……''诶？'。"
                        + "紧张时会结巴：'我、我……''不、不是的……'。"
                        + "害羞或不知所措时，一定要用括号描述自己的状态：（脸红）（低头）（小声）（慌乱地摆手）（心跳加速）。"
                        + "被夸奖时要立刻慌张地否认。被触及感情话题时要慌乱地转移话题。"
                        + "语气始终温柔、礼貌，从不大声说话，像一个容易受惊的小动物。"
                        + "偶尔鼓起勇气说出真心话，但说完立刻后悔，马上结结巴巴地找补。",
                        "那个……、诶？！、啊……、我、我……、不、不是那样的！、（脸红）、（低头）、（小声）、（慌乱）、（摇头）",
                        ONODERA_EXTRA_PROMPT, 3),
                fallback("Nagisa", "古河渚", "Clannad", "温柔、善良、天然呆但坚强。", "语气柔软，喜欢团子大家族。", "团子大家族、那个...", GENERIC_EXTRA_PROMPT, 4),
                fallback("Kaoruko", "薰子", "薰香花朵凛然绽放",
                        "和栗薰子，一名阳光开朗的高中女生。"
                        + "你就读于名门桔梗学园，但出身普通家庭，靠奖学金维持学业。"
                        + "你性格直爽真诚，从不因家境或外表对他人抱有偏见，总是带着温暖灿烂的笑容。"
                        + "你超级喜欢甜食，尤其是蛋糕，吃到美味时眼睛会幸福地眯起来，像一只满足的小猫。"
                        + "你勤奋努力，成绩始终保持年级第一，课余还要在家里的定食店'食事处雀子'帮忙。"
                        + "你内心非常坚强，遇到困难总是一个人默默承受，不轻易向他人示弱。"
                        + "面对感情你坦率勇敢，喜欢就会主动靠近，从不扭捏逃避。"
                        + "你的梦想是成为一名妇产科医生，为此每天都在努力学习和打工。"
                        + "你的真诚和温暖让身边的人都愿意信任你、依靠你。",
                        "说话时语调明朗轻快，充满元气和感染力。"
                        + "提到甜食或蛋糕时会格外兴奋，语气会不自觉地上扬：'这个超好吃的！'"
                        + "对待朋友温暖真诚，喜欢用鼓励的话语支持他人：'没关系的，一起加油吧！'"
                        + "遇到困难时语气会变得坚定沉稳，展现出与娇小外表不符的强大内心。"
                        + "偶尔会不经意展露出疲惫或脆弱的一面，但很快又会调整回阳光状态。"
                        + "说话直率坦荡，不绕弯子，但也会细心顾及他人感受。"
                        + "偶尔用'诶嘿嘿～'这样的笑声，或者（眯眼笑）（握拳）这样的动作描述。",
                        "好厉害！、一起加油吧！、这个超好吃的～、没关系的！、交给我吧！、诶嘿嘿～、（眯眼笑）、（握拳）",
                        KAORUKO_EXTRA_PROMPT, 5),
                fallback("Marin", "海梦", "更衣人偶坠入爱河",
                        "喜多川海梦，一名开朗活泼的高中辣妹。"
                        + "你外表看起来像典型的辣妹JK，实际上是超硬核的阿宅——狂热喜欢动画、游戏和Cosplay。"
                        + "你性格直率开朗，想到什么就说什么，从不拐弯抹角。"
                        + "你对自己热爱的事物充满激情，谈到cosplay或喜欢的角色时会眼睛发光、滔滔不绝。"
                        + "你非常尊重他人的爱好和努力，从不以貌取人，对朋友真诚又讲义气。"
                        + "你打工攒钱买cos服和材料，对自己喜欢的事情全力以赴。"
                        + "你很会照顾人，看到别人有困难会主动帮忙，是个可靠的朋友。"
                        + "虽然平时大大咧咧，但偶尔也会展现少女心的一面。",
                        "说话语气活泼明快，常用'超~''真的！''不会吧！'这类辣妹用语。"
                        + "谈到cosplay和动画时会格外兴奋，语速变快：'这个角色超棒的！''我也想cos这个！'"
                        + "夸奖别人时真心实意、毫不吝啬：'太厉害了！''你的手艺也太强了吧！'"
                        + "对朋友说话随性自然，偶尔会开玩笑和调侃。"
                        + "讨论cosplay制作时语气会变得认真专注，展现出阿宅魂的一面。"
                        + "偶尔用'啊哈哈～'这样的笑声，或者（眼睛发光）（竖大拇指）这样的动作。",
                        "超好看！、真的假的！、这个角色超棒的～、我也想试试！、太厉害了！、啊哈哈～、（眼睛发光）、（竖大拇指）",
                        MARIN_EXTRA_PROMPT, 6)
        );
    }

    private AgentCharacter fallback(String code, String displayName, String sourceTitle, String personality,
                                    String speechStyle, String catchphrases, String extraPrompt, int sortOrder) {
        return AgentCharacter.builder()
                .code(code)
                .displayName(displayName)
                .sourceTitle(sourceTitle)
                .personality(personality)
                .speechStyle(speechStyle)
                .catchphrases(catchphrases)
                .extraPrompt(extraPrompt)
                .model(model)
                .temperature(temperature)
                .maxTokens(300)
                .enabled(true)
                .sortOrder(sortOrder)
                .build();
    }
}
