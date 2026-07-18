package com.aniglow.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = parseJwt(request);

        if (jwt != null) {
            try {
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.extractUsername(jwt);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // validateJwtToken returned false — determine why by trying to extract claims
                    try {
                        jwtUtils.extractUsername(jwt);
                        // Token parsed OK but expired — validateJwtToken checks isTokenExpired
                        request.setAttribute("jwt.error", "expired");
                    } catch (ExpiredJwtException e) {
                        request.setAttribute("jwt.error", "expired");
                    } catch (MalformedJwtException | SecurityException
                             | UnsupportedJwtException | IllegalArgumentException e) {
                        request.setAttribute("jwt.error", "invalid");
                    }
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute("jwt.error", "expired");
            } catch (MalformedJwtException | SecurityException
                     | UnsupportedJwtException | IllegalArgumentException e) {
                request.setAttribute("jwt.error", "invalid");
            } catch (Exception e) {
                log.error("无法设置用户认证: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
