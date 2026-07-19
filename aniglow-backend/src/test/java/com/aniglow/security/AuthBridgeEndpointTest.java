package com.aniglow.security;

import com.aniglow.dto.auth.AuthResponse;
import com.aniglow.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthBridgeEndpointTest {

    private static final String BRIDGE_SECRET = "test-auth-bridge-secret-32-characters-minimum";
    private static final String REQUEST_BODY = """
            {"authingId":"verified-authing-user","phone":"13800138000"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void rejectsDirectAuthingLoginWithoutServerSecret() throws Exception {
        mockMvc.perform(post("/auth/authing-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsRequestsFromTheAuthenticatedNuxtBridge() throws Exception {
        when(authService.authingLogin(any())).thenReturn(AuthResponse.builder()
                .id(42L)
                .token("backend-jwt")
                .build());

        mockMvc.perform(post("/auth/authing-login")
                        .header("X-Auth-Bridge-Secret", BRIDGE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk());
    }
}
