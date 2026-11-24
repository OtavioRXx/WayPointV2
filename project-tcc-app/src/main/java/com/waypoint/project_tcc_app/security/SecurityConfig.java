package com.waypoint.project_tcc_app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/linhas", "/paradas", "/ocorrencias", "/reportes", "/notificacoes").permitAll()
                        .requestMatchers("/login", "/cadastro", "/cadastrar").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/error/403").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers("/painel-testes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/admins").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/linhas/*/reportes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/linhas/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/linhas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/linhas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/linhas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/paradas/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/paradas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/paradas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/paradas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ocorrencias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ocorrencias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/ocorrencias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ocorrencias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/feed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reportes/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/reportes/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/*/reportes").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/usuarios/*/linhas-favoritas/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes/publicas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes/avisos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes", "/api/notificacoes/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/notificacoes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/notificacoes/avisos/*/superada", "/api/notificacoes/avisos/oficial/*/superada").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/minhas/historico").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*/notificacoes").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/notificacoes/*/lida").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403")
                )
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .frameOptions(frame -> frame.deny())
                )
                .addFilterBefore(
                        new LoginBloqueadoFilter(loginAttemptService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.registrarSucesso(authentication.getName());
                            LOGGER.info("evento=LOGIN_SUCESSO usuario={}", authentication.getName());
                            response.sendRedirect(request.getContextPath() + "/home");
                        })
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            loginAttemptService.registrarFalha(username);
                            LOGGER.warn("evento=LOGIN_FALHA usuario={}", username);
                            response.sendRedirect(request.getContextPath() + "/login?erro");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
