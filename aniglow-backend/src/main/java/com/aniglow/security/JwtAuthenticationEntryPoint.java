package com.aniglow.security;

import com.aniglow.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String jwtError = (String) request.getAttribute("jwt.error");
        String message;
        if ("expired".equals(jwtError)) {
            message = "登录已过期，请重新登录";
        } else if ("invalid".equals(jwtError)) {
            message = "Token无效";
        } else if (authException != null && authException.getMessage() != null) {
            message = "请先登录";
        } else {
            message = "请先登录";
        }
        log.warn("未授权: {} (jwt.error={})", message, jwtError);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(message));
    }
}
