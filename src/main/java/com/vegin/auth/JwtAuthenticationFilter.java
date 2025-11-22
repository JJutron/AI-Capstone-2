package com.vegin.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api/auth"); // 로그인/회원가입
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token == null) {
                log.warn("JWT reject: missing Authorization header");
                chain.doFilter(request, response);
                return;
            }
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("JWT reject: invalid token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = jwtTokenProvider.extractUserId(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId); // 반드시 구현돼 있어야 함

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT reject: expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT reject: bad signature");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            log.warn("JWT reject: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            // 중요: 401일 때 여기 로그로 바로 알 수 있음
            log.warn("JWT reject: missing Authorization header");
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            log.warn("JWT reject: invalid scheme header={}", header);
            return null;
        }
        return header.substring(7);
    }
}