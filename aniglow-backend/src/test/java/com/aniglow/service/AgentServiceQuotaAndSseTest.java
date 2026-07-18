package com.aniglow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Agent 额度控制与 SSE 流式")
class AgentServiceQuotaAndSseTest {

    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final SetOperations<String, Object> setOps = mock(SetOperations.class);

    private final AgentService agentService = new AgentService(
            redisTemplate,
            null, // agentCharacterRepository
            null, // animeRepository
            new ObjectMapper()
    );

    private Method consumeQuotaMethod;
    private Method peekQuotaMethod;
    private Method resolveQuotaLimitMethod;
    private Method isPaidUserMethod;
    private Method resolveMembershipMethod;
    private Method quotaKeyMethod;
    private Method nextQuotaResetAtMethod;
    private Method buildQuotaEventMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        consumeQuotaMethod = AgentService.class.getDeclaredMethod("consumeQuota", String.class);
        consumeQuotaMethod.setAccessible(true);

        peekQuotaMethod = AgentService.class.getDeclaredMethod("peekQuota", String.class);
        peekQuotaMethod.setAccessible(true);

        resolveQuotaLimitMethod = AgentService.class.getDeclaredMethod("resolveQuotaLimit", String.class);
        resolveQuotaLimitMethod.setAccessible(true);

        isPaidUserMethod = AgentService.class.getDeclaredMethod("isPaidUser", String.class);
        isPaidUserMethod.setAccessible(true);

        resolveMembershipMethod = AgentService.class.getDeclaredMethod("resolveMembership", String.class);
        resolveMembershipMethod.setAccessible(true);

        quotaKeyMethod = AgentService.class.getDeclaredMethod("quotaKey", String.class);
        quotaKeyMethod.setAccessible(true);

        nextQuotaResetAtMethod = AgentService.class.getDeclaredMethod("nextQuotaResetAt");
        nextQuotaResetAtMethod.setAccessible(true);

        buildQuotaEventMethod = AgentService.class.getDeclaredMethod("buildQuotaEvent",
                Class.forName("com.aniglow.service.AgentService$QuotaResult"));
        buildQuotaEventMethod.setAccessible(true);
    }

    private Object invoke(Method method, Object... args) throws Exception {
        try {
            return method.invoke(agentService, args);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 额度控制
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("额度消耗 — consumeQuota")
    class ConsumeQuota {

        @Test
        @DisplayName("首次调用 increment 返回 1 → remaining=limit-1")
        void firstConsumeReturnsLimitMinusOne() throws Exception {
            when(valueOps.increment(anyString())).thenReturn(1L);

            Object result = invoke(consumeQuotaMethod, "test-user-1");

            assertThat(result.toString()).contains("remaining=9");
            assertThat(result.toString()).contains("limited=false");
            verify(valueOps).increment(anyString());
        }

        @Test
        @DisplayName("increment 返回 limit+1 → limited=true")
        void overLimitReturnsLimited() throws Exception {
            when(valueOps.increment(anyString())).thenReturn(11L);

            Object result = invoke(consumeQuotaMethod, "test-user-1");

            assertThat(result.toString()).contains("remaining=0");
            assertThat(result.toString()).contains("limited=true");
        }

        @Test
        @DisplayName("首次写入时设置 TTL 到次日零点")
        void firstWriteSetsExpiry() throws Exception {
            when(valueOps.increment(anyString())).thenReturn(1L);

            invoke(consumeQuotaMethod, "test-user-1");

            verify(redisTemplate).expire(anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Redis increment 异常时放行，返回满额")
        void redisExceptionAllowsPass() throws Exception {
            when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

            Object result = invoke(consumeQuotaMethod, "test-user-1");

            assertThat(result.toString()).contains("limited=false");
            assertThat(result.toString()).contains("remaining=10");
        }
    }

    @Nested
    @DisplayName("额度查询 — peekQuota")
    class PeekQuota {

        @Test
        @DisplayName("查询已用次数 → remaining 正确计算")
        void peekReturnsRemaining() throws Exception {
            when(valueOps.get(anyString())).thenReturn(5);

            Object result = invoke(peekQuotaMethod, "test-user-1");

            assertThat(result.toString()).contains("remaining=5");
        }

        @Test
        @DisplayName("Redis 无记录 → remaining=limit")
        void noRecordReturnsFullQuota() throws Exception {
            when(valueOps.get(anyString())).thenReturn(null);

            Object result = invoke(peekQuotaMethod, "test-user-1");

            assertThat(result.toString()).contains("remaining=10");
        }
    }

    @Nested
    @DisplayName("额度上限 — resolveQuotaLimit")
    class ResolveQuotaLimit {

        @Test
        @DisplayName("anonymous 用户 → 10")
        void anonymousReturnsFreeQuota() throws Exception {
            int limit = (int) invoke(resolveQuotaLimitMethod, "anonymous");
            assertThat(limit).isEqualTo(10);
        }

        @Test
        @DisplayName("普通用户 → 10")
        void normalUserReturnsFreeQuota() throws Exception {
            when(valueOps.get(anyString())).thenReturn(null);
            when(setOps.isMember(anyString(), anyString())).thenReturn(false);

            int limit = (int) invoke(resolveQuotaLimitMethod, "normal-user");
            assertThat(limit).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("付费用户判断 — isPaidUser")
    class IsPaidUser {

        @Test
        @DisplayName("null/空字符串 → false")
        void nullOrEmptyReturnsFalse() throws Exception {
            assertThat(invoke(isPaidUserMethod, (String) null)).isEqualTo(false);
            assertThat(invoke(isPaidUserMethod, "")).isEqualTo(false);
        }

        @Test
        @DisplayName("anonymous → false")
        void anonymousReturnsFalse() throws Exception {
            assertThat(invoke(isPaidUserMethod, "anonymous")).isEqualTo(false);
        }

        @Test
        @DisplayName("白名单用户 → true")
        void whitelistedUserReturnsTrue() throws Exception {
            when(valueOps.get(anyString())).thenReturn(null); // membership
            when(setOps.isMember(anyString(), eq("paid-user"))).thenReturn(true);

            assertThat(invoke(isPaidUserMethod, "paid-user")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("会员状态 — resolveMembership")
    class ResolveMembership {

        @Test
        @DisplayName("无会员记录 → inactive")
        void noRecordReturnsInactive() throws Exception {
            when(valueOps.get(anyString())).thenReturn(null);

            Object result = invoke(resolveMembershipMethod, "test-user");
            assertThat(result.toString()).contains("active=false");
        }

        @Test
        @DisplayName("已过期会员 → inactive")
        void expiredReturnsInactive() throws Exception {
            String pastDate = LocalDateTime.now().minusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            when(valueOps.get(anyString())).thenReturn(pastDate);

            Object result = invoke(resolveMembershipMethod, "test-user");
            assertThat(result.toString()).contains("active=false");
        }

        @Test
        @DisplayName("有效期内会员 → active")
        void activeReturnsActive() throws Exception {
            String futureDate = LocalDateTime.now().plusDays(10)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            when(valueOps.get(anyString())).thenReturn(futureDate);

            Object result = invoke(resolveMembershipMethod, "test-user");
            assertThat(result.toString()).contains("active=true");
        }
    }

    @Nested
    @DisplayName("配额 Key 生成")
    class QuotaKey {

        @Test
        @DisplayName("quotaKey 包含 uid 和日期")
        void quotaKeyContainsUidAndDate() throws Exception {
            String key = (String) invoke(quotaKeyMethod, "my-uid");
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertThat(key).contains("my-uid");
            assertThat(key).contains(today);
        }

        @Test
        @DisplayName("nextQuotaResetAt 返回下一个整点")
        void nextQuotaResetAtIsNextFullHour() throws Exception {
            LocalDateTime before = LocalDateTime.now();
            LocalDateTime reset = (LocalDateTime) invoke(nextQuotaResetAtMethod);
            LocalDateTime after = LocalDateTime.now();

            assertThat(reset.getMinute()).isEqualTo(0);
            assertThat(reset.getSecond()).isEqualTo(0);
            assertThat(reset.getNano()).isEqualTo(0);
            assertThat(reset).isAfter(before);
            assertThat(reset).isBeforeOrEqualTo(after.plusHours(1));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SSE 流式
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SSE 流式对话 — chatStream")
    class SseStreaming {

        @Test
        @DisplayName("API key 为空时 emitter 返回非 null")
        void emptyApiKeySendsFallback() {
            String currentApiKey = (String) ReflectionTestUtils.getField(agentService, "apiKey");
            assertThat(currentApiKey).isNullOrEmpty();

            var req = createRequest("Rem", "你好");
            var emitter = agentService.chatStream(req, 1L);
            assertThat(emitter).isNotNull();
        }

        @Test
        @DisplayName("SseEmitter 超时设为 120 秒")
        void sseEmitterTimeoutIs120Seconds() {
            var req = createRequest("Rem", "你好");
            var emitter = agentService.chatStream(req, 1L);
            assertThat(emitter.getTimeout()).isEqualTo(120_000L);
        }

        @Test
        @DisplayName("buildQuotaEvent 格式正确")
        void buildQuotaEventFormat() throws Exception {
            when(valueOps.increment(anyString())).thenReturn(1L);
            Object quotaResult = invoke(consumeQuotaMethod, "test-user");

            String event = (String) invoke(buildQuotaEventMethod, quotaResult);
            assertThat(event).contains("limit=");
            assertThat(event).contains("remaining=");
            assertThat(event).contains("resetAt=");
        }

        private com.aniglow.dto.agent.AgentChatRequest createRequest(String role, String message) {
            var req = new com.aniglow.dto.agent.AgentChatRequest();
            req.setRole(role);
            req.setMessage(message);
            return req;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 意图覆盖度（补充边界用例）
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("意图识别边界 — resolveCommonIntent 补充")
    class IntentCoverage {

        private Method resolveCommonIntentMethod;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            resolveCommonIntentMethod = AgentService.class.getDeclaredMethod(
                    "resolveCommonIntent", String.class);
            resolveCommonIntentMethod.setAccessible(true);
        }

        private String intent(String message) throws Exception {
            try {
                return (String) resolveCommonIntentMethod.invoke(agentService, message);
            } catch (InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
        }

        @Test
        @DisplayName("\"喜欢你\" -> love (不被 praise 关键词冲突)")
        void likeYouIsLove() throws Exception {
            assertThat(intent("喜欢你")).isEqualTo("love");
        }

        @Test
        @DisplayName("\"你好厉害\" -> praise (不被 hello 误匹配)")
        void niHaoLiHaiIsPraise() throws Exception {
            assertThat(intent("你好厉害")).isEqualTo("praise");
        }

        @Test
        @DisplayName("\"你好温柔\" -> praise")
        void niHaoWenRouIsPraise() throws Exception {
            assertThat(intent("你好温柔")).isEqualTo("praise");
        }

        @Test
        @DisplayName("标点清洗后匹配: 谢，谢。 -> thanks")
        void punctuationCleanedThanks() throws Exception {
            assertThat(intent("谢，谢。")).isEqualTo("thanks");
        }

        @Test
        @DisplayName("标点清洗后匹配: 晚，安～ -> goodnight")
        void punctuationCleanedGoodnight() throws Exception {
            assertThat(intent("晚，安～")).isEqualTo("goodnight");
        }

        @Test
        @DisplayName("大小写混合: THANK YOU -> thanks")
        void caseInsensitiveThanks() throws Exception {
            assertThat(intent("THANK YOU")).isEqualTo("thanks");
        }

        @Test
        @DisplayName("空字符串 -> 空")
        void emptyReturnsEmpty() throws Exception {
            assertThat(intent("")).isEmpty();
        }

        @Test
        @DisplayName("纯标点 -> 空")
        void onlyPunctuationReturnsEmpty() throws Exception {
            assertThat(intent("，。！？")).isEmpty();
        }

        @Test
        @DisplayName("忙什么呢 -> what_doing")
        void mangShenMeNe() throws Exception {
            assertThat(intent("忙什么呢")).isEqualTo("what_doing");
        }

        @Test
        @DisplayName("我要睡了 -> goodnight")
        void woYaoShuiLe() throws Exception {
            assertThat(intent("我要睡了")).isEqualTo("goodnight");
        }
    }
}
