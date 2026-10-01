package com.aniglow.service;

import com.aniglow.entity.AgentCharacter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Agent 情感标签解析与意图识别")
class AgentServiceEmotionTest {

    private final AgentService agentService = new AgentService(
            mock(RedisTemplate.class),
            null, // stringRedisTemplate
            null, // agentCharacterRepository
            null, // animeRepository
            new ObjectMapper(),
            null // chatClient（本测试只验证纯逻辑，不触达模型）
    );

    private Method parseEmotionMethod;
    private Method resolveCommonIntentMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        parseEmotionMethod = AgentService.class.getDeclaredMethod("parseEmotion", String.class);
        parseEmotionMethod.setAccessible(true);

        resolveCommonIntentMethod = AgentService.class.getDeclaredMethod("resolveCommonIntent", String.class);
        resolveCommonIntentMethod.setAccessible(true);
    }

    private Object invokeParseEmotion(String raw) throws Exception {
        try {
            return parseEmotionMethod.invoke(agentService, raw);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private String invokeResolveCommonIntent(String message) throws Exception {
        try {
            return (String) resolveCommonIntentMethod.invoke(agentService, message);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Nested
    @DisplayName("情感标签解析 — parseEmotion")
    class ParseEmotion {

        @Test
        @DisplayName("[happy]标签 → emotion=happy, content=内容")
        void happyEmotion() throws Exception {
            Object result = invokeParseEmotion("[happy]今天真是个好日子呢！");

            assertThat(result).isNotNull();
            // ParsedMessage is a record with emotion() and content()
            assertThat(result.toString()).contains("happy");
        }

        @Test
        @DisplayName("[normal]标签 → emotion=normal, content=内容")
        void normalEmotion() throws Exception {
            Object result = invokeParseEmotion("[normal]今天天气不错。");

            assertThat(result.toString()).contains("normal");
        }

        @Test
        @DisplayName("[complex]标签 → emotion=complex, content=内容")
        void complexEmotion() throws Exception {
            Object result = invokeParseEmotion("[complex]诶？！你、你在说什么呀...");

            assertThat(result.toString()).contains("complex");
        }

        @Test
        @DisplayName("无标签 → 默认 normal")
        void noTagDefaultsToNormal() throws Exception {
            Object result = invokeParseEmotion("你好，今天过得怎么样？");

            assertThat(result.toString()).contains("normal");
        }

        @Test
        @DisplayName("多行内容含标签 → 正确解析第一行标签")
        void multilineWithTag() throws Exception {
            Object result = invokeParseEmotion("[happy]第一段内容\n第二段内容");

            assertThat(result.toString()).contains("happy");
        }

        @Test
        @DisplayName("标签在中间 → 不被识别为标签")
        void tagInMiddleIsNotParsed() throws Exception {
            Object result = invokeParseEmotion("我今天很[happy]开心");

            // 不会识别，因为标签必须在最开头
            assertThat(result.toString()).contains("normal");
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("空/空白/null → emotion=normal, content 为空")
        void nullOrBlankInput(String input) throws Exception {
            Object result = invokeParseEmotion(input);

            assertThat(result.toString()).contains("normal");
        }

        @Test
        @DisplayName("仅包含标签 → 标签解析正确")
        void onlyTag() throws Exception {
            Object result = invokeParseEmotion("[normal]");

            assertThat(result.toString()).contains("normal");
        }

        @Test
        @DisplayName("未知标签名 → 默认 normal")
        void unknownTagDefaultsToNormal() throws Exception {
            Object result = invokeParseEmotion("[angry]真让人生气！");

            assertThat(result.toString()).contains("normal");
        }
    }

    @Nested
    @DisplayName("常见意图识别 — resolveCommonIntent")
    class ResolveCommonIntent {

        @Test
        @DisplayName("你好 → hello")
        void helloIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("你好")).isEqualTo("hello");
            assertThat(invokeResolveCommonIntent("您好，在吗？")).isEqualTo("hello");
            assertThat(invokeResolveCommonIntent("hi!")).isEqualTo("hello");
            assertThat(invokeResolveCommonIntent("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("你在干嘛 → what_doing")
        void whatDoingIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("你在干什么")).isEqualTo("what_doing");
            assertThat(invokeResolveCommonIntent("你在干嘛呢")).isEqualTo("what_doing");
            assertThat(invokeResolveCommonIntent("在做什么")).isEqualTo("what_doing");
            assertThat(invokeResolveCommonIntent("忙什么")).isEqualTo("what_doing");
        }

        @Test
        @DisplayName("谢谢 → thanks")
        void thanksIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("谢谢")).isEqualTo("thanks");
            assertThat(invokeResolveCommonIntent("感谢你")).isEqualTo("thanks");
            assertThat(invokeResolveCommonIntent("thank you")).isEqualTo("thanks");
        }

        @Test
        @DisplayName("晚安 → goodnight")
        void goodnightIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("晚安")).isEqualTo("goodnight");
            assertThat(invokeResolveCommonIntent("睡觉了")).isEqualTo("goodnight");
            assertThat(invokeResolveCommonIntent("goodnight")).isEqualTo("goodnight");
            assertThat(invokeResolveCommonIntent("我睡了")).isEqualTo("goodnight");
        }

        @Test
        @DisplayName("想你 → miss_you")
        void missYouIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("想你")).isEqualTo("miss_you");
            assertThat(invokeResolveCommonIntent("想你了")).isEqualTo("miss_you");
            assertThat(invokeResolveCommonIntent("好想你")).isEqualTo("miss_you");
            assertThat(invokeResolveCommonIntent("miss you")).isEqualTo("miss_you");
        }

        @Test
        @DisplayName("夸奖 → praise")
        void praiseIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("好厉害")).isEqualTo("praise");
            assertThat(invokeResolveCommonIntent("真棒")).isEqualTo("praise");
            assertThat(invokeResolveCommonIntent("太强了")).isEqualTo("praise");
            assertThat(invokeResolveCommonIntent("好可爱")).isEqualTo("praise");
        }

        @Test
        @DisplayName("喜欢/爱 → love")
        void loveIntent() throws Exception {
            assertThat(invokeResolveCommonIntent("我爱你")).isEqualTo("love");
            assertThat(invokeResolveCommonIntent("喜欢你")).isEqualTo("love");
            assertThat(invokeResolveCommonIntent("在一起")).isEqualTo("love");
            assertThat(invokeResolveCommonIntent("love you")).isEqualTo("love");
        }

        @Test
        @DisplayName("你好不再误匹配夸奖短语")
        void helloDoesNotStealPraise() throws Exception {
            assertThat(invokeResolveCommonIntent("你好厉害")).isEqualTo("praise");
        }

        @Test
        @DisplayName("无匹配 → 空字符串")
        void unmatchedReturnsEmpty() throws Exception {
            assertThat(invokeResolveCommonIntent("今天天气怎么样")).isEmpty();
            assertThat(invokeResolveCommonIntent("推荐一部动漫")).isEmpty();
        }

        @Test
        @DisplayName("空白输入 → 空字符串")
        void blankReturnsEmpty() throws Exception {
            assertThat(invokeResolveCommonIntent("")).isEmpty();
            assertThat(invokeResolveCommonIntent("   ")).isEmpty();
        }

        @Test
        @DisplayName("praise优先级低于love（不含爱/喜欢关键词的夸奖 → praise）")
        void praiseWithLikeButNotLove() throws Exception {
            // "好温柔" 匹配 praise (好温柔)，也匹配 love (没有明确的爱/喜欢)
            assertThat(invokeResolveCommonIntent("好温柔")).isEqualTo("praise");
        }
    }

    @Nested
    @DisplayName("热门角色常见回复缓存")
    class HotRoleCommonReplies {

        @Test
        @DisplayName("蕾姆和小野寺是热门角色")
        void remAndOnoderaAreHotRoles() {
            // 通过反射获取 HOT_ROLE_CODES
            Object hotRoleCodes = ReflectionTestUtils.getField(agentService, "HOT_ROLE_CODES");
            assertThat(hotRoleCodes).isNotNull();
            @SuppressWarnings("unchecked")
            var codes = (java.util.Set<String>) hotRoleCodes;
            assertThat(codes).contains("Rem", "Onodera");
        }

        @Test
        @DisplayName("默认常见回复配置存在")
        void defaultCommonRepliesExist() {
            Object replies = ReflectionTestUtils.getField(agentService, "DEFAULT_COMMON_REPLIES");
            assertThat(replies).isNotNull();
            @SuppressWarnings("unchecked")
            var replyMap = (Map<String, Map<String, String>>) replies;
            assertThat(replyMap).containsKeys("Rem", "Onodera");
            assertThat(replyMap.get("Rem")).containsKeys("hello", "thanks", "goodnight", "what_doing");
            assertThat(replyMap.get("Onodera")).containsKeys(
                    "hello", "thanks", "goodnight", "miss_you", "praise", "love");
        }
    }
}
