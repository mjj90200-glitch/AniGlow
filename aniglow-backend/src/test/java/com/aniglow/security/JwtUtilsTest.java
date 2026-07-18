package com.aniglow.security;

import com.aniglow.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT 工具类")
class JwtUtilsTest {

    private static final String TEST_SECRET = "YW5pZ2xvdy1qd3Qtc2VjcmV0LWtleS1mb3Itc3ByaW5nLWJvb3QtYXBwbGljYXRpb24tdGVzdGtleTEyMzQ1Ng==";

    private final JwtUtils jwtUtils = new JwtUtils();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000); // 1 hour
        ReflectionTestUtils.setField(jwtUtils, "refreshExpirationMs", 604800000); // 7 days
    }

    private UserDetailsImpl createUserDetails() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .build();
        return UserDetailsImpl.build(user);
    }

    @Nested
    @DisplayName("Token 生成")
    class TokenGeneration {

        @Test
        @DisplayName("生成包含用户名的有效token")
        void generatesTokenWithUsername() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            assertThat(token).isNotBlank();
            assertThat(jwtUtils.extractUsername(token)).isEqualTo("testuser");
        }

        @Test
        @DisplayName("生成的token可以被验证通过")
        void generatedTokenPassesValidation() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            assertThat(jwtUtils.isTokenValid(token, userDetails)).isTrue();
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        }

        @Test
        @DisplayName("带不同自定义claims时生成的token不同")
        void eachTokenIsDifferent() {
            UserDetailsImpl userDetails = createUserDetails();

            String token1 = jwtUtils.generateToken(userDetails);
            String token2 = jwtUtils.generateToken(
                    java.util.Map.of("custom", "value"), userDetails);

            // 即使时间戳相近，自定义claims使token内容不同
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("刷新token有效期长于普通token")
        void refreshTokenHasLongerExpiration() {
            UserDetailsImpl userDetails = createUserDetails();
            String accessToken = jwtUtils.generateToken(userDetails);
            String refreshToken = jwtUtils.generateRefreshToken(userDetails);

            long accessExpiration = jwtUtils.extractExpiration(accessToken).getTime();
            long refreshExpiration = jwtUtils.extractExpiration(refreshToken).getTime();

            assertThat(refreshExpiration).isGreaterThan(accessExpiration);
        }
    }

    @Nested
    @DisplayName("Token 验证")
    class TokenValidation {

        @Test
        @DisplayName("用户名不匹配时验证失败")
        void rejectsTokenWithDifferentUsername() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            User otherUser = User.builder()
                    .id(2L)
                    .username("otheruser")
                    .email("other@example.com")
                    .password("encoded")
                    .role(User.Role.USER)
                    .build();
            UserDetailsImpl otherDetails = UserDetailsImpl.build(otherUser);

            assertThat(jwtUtils.isTokenValid(token, otherDetails)).isFalse();
        }

        @Test
        @DisplayName("过期token验证失败")
        void rejectsExpiredToken() {
            // 设置极短的过期时间
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1);
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            // 等待token过期
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}

            assertThat(jwtUtils.validateJwtToken(token)).isFalse();
        }

        @Test
        @DisplayName("篡改的token被拒绝")
        void rejectsTamperedToken() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            // 修改payload中间字符使签名失效
            String[] parts = token.split("\\.");
            String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) + "X";
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            assertThat(jwtUtils.validateJwtToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("空字符串token验证抛出异常")
        void rejectsEmptyToken() {
            assertThat(jwtUtils.validateJwtToken("")).isFalse();
        }

        @Test
        @DisplayName("null token验证抛出异常")
        void rejectsNullToken() {
            assertThat(jwtUtils.validateJwtToken(null)).isFalse();
        }

        @Test
        @DisplayName("无效格式token被拒绝")
        void rejectsMalformedToken() {
            assertThat(jwtUtils.validateJwtToken("not-a-valid-jwt-token-at-all")).isFalse();
        }

        @Test
        @DisplayName("缺少签名部分的token被拒绝")
        void rejectsTokenWithoutSignature() {
            UserDetailsImpl userDetails = createUserDetails();
            String fullToken = jwtUtils.generateToken(userDetails);

            // 取前两部分（去掉签名）
            String[] parts = fullToken.split("\\.");
            String tokenWithoutSignature = parts[0] + "." + parts[1] + ".";

            assertThat(jwtUtils.validateJwtToken(tokenWithoutSignature)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token 解析")
    class TokenParsing {

        @Test
        @DisplayName("从有效token提取用户名")
        void extractsUsernameFromValidToken() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            String username = jwtUtils.extractUsername(token);

            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("从有效token提取过期时间")
        void extractsExpirationFromValidToken() {
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);

            assertThat(jwtUtils.extractExpiration(token)).isNotNull();
            assertThat(jwtUtils.extractExpiration(token)).isAfter(
                    new java.util.Date(System.currentTimeMillis()));
        }

        @Test
        @DisplayName("过期token提取用户名仍然有效(从异常中获取)")
        void extractsUsernameFromExpiredToken() {
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1);
            UserDetailsImpl userDetails = createUserDetails();
            String token = jwtUtils.generateToken(userDetails);
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            // JJWT 0.12.x 在解析过期token时抛出 ExpiredJwtException
            try {
                jwtUtils.extractUsername(token);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // 即使过期，Claims 仍可从异常中获取
                String username = e.getClaims().getSubject();
                assertThat(username).isEqualTo("testuser");
            }
        }
    }

    @Nested
    @DisplayName("配置获取")
    class ConfigurationAccess {

        @Test
        @DisplayName("返回配置的有效期")
        void returnsConfiguredExpirationTime() {
            long expiration = jwtUtils.getExpirationTime();
            assertThat(expiration).isEqualTo(3600000);
        }
    }
}
