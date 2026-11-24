package com.waypoint.project_tcc_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class LoginBloqueadoFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginBloqueadoFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String username = request.getParameter("username");
        if ("POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + "/login").equals(request.getRequestURI())
                && loginAttemptService.estaBloqueado(username)) {
            response.sendRedirect(request.getContextPath() + "/login?erro");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
