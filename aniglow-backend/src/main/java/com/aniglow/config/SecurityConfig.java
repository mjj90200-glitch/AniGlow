package com.aniglow.config;

import com.aniglow.security.JwtAuthenticationEntryPoint;
import com.aniglow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception ->
                    exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                    // Swagger / OpenAPI
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**").hasRole("ADMIN")
                    // 公开 API（context-path 已是 /api，此处路径无需再写 /api 前缀）
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/anime/**").permitAll()
                    .requestMatchers("/ranking/**").permitAll()
                    .requestMatchers("/ratings/anime/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/ratings/*/replies").permitAll()
                    .requestMatchers(HttpMethod.GET, "/agent/roles", "/agent/characters", "/agent/characters/anime/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/agent/vote/candidates").permitAll()
                    .requestMatchers("/stats/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/communities/**").permitAll()
                    // 需要认证的 API
                    .requestMatchers(HttpMethod.POST, "/votes/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/ratings/**").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/ratings/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/ratings/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/communities/**").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/communities/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/communities/**").authenticated()
                    .requestMatchers("/agent/**").authenticated()
                    .requestMatchers("/user/**").authenticated()
                    // 管理员权限
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://127.0.0.1:3001", "http://localhost:3000", "http://localhost:3001", "http://localhost:8081",
                "https://www.mjj520.top", "http://www.mjj520.top"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
