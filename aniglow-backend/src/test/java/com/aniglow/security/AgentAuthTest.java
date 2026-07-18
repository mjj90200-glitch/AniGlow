package com.aniglow.security;

import com.aniglow.dto.agent.AgentChatResponse;
import com.aniglow.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @Test
    void chatRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"你好","role":"Rem","userId":"attacker"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chatUsesAuthenticatedPrincipalInsteadOfRequestUserId() throws Exception {
        when(agentService.chat(any(), eq(42L))).thenReturn(AgentChatResponse.builder()
                .role("Rem")
                .roleName("蕾姆")
                .reply("你好")
                .emotion("normal")
                .build());

        mockMvc.perform(post("/agent/chat")
                        .with(authentication(userAuthentication(42L, "ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"你好","role":"Rem","userId":"attacker"}
                                """))
                .andExpect(status().isOk());

        verify(agentService).chat(any(), eq(42L));
    }

    @Test
    void monthlyActivationRejectsRegularUsers() throws Exception {
        mockMvc.perform(post("/agent/membership/activate-monthly")
                        .with(authentication(userAuthentication(42L, "ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken userAuthentication(Long id, String role) {
        UserDetailsImpl user = new UserDetailsImpl(
                id,
                "test-user",
                "test@example.com",
                "unused",
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
