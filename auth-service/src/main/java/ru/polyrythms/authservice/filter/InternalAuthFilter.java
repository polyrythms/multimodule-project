package ru.polyrythms.authservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${internal.auth.key}")
    private String internalAuthKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // Пропускаем JWKS endpoint и health без проверки
        if (path.equals("/.well-known/jwks.json") || path.equals("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("X-Internal-Auth");
        if (authHeader == null || !authHeader.equals(internalAuthKey)) {
            log.warn("Unauthorized access attempt to {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or missing X-Internal-Auth header");
            return;
        }

        filterChain.doFilter(request, response);
    }
}