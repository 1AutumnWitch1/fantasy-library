package com.fantasylibrary.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fantasylibrary.api.ApiResponse;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.service.AccountService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Configuration(proxyBeanMethods = false)
class LibrarySecurity {
    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CookieCsrfTokenRepository csrfTokens(@Value("${library.auth.secure-cookie:false}") boolean secure) {
        return csrfRepository(secure);
    }

    @Bean
    CookieCsrfTokenRepository adminCsrfTokens(@Value("${library.auth.secure-cookie:false}") boolean secure) {
        var tokens = csrfRepository(secure);
        tokens.setCookieName("FANTASY-ADMIN-XSRF");
        tokens.setCookiePath("/api/admin");
        tokens.setHeaderName("X-Admin-CSRF-Token");
        return tokens;
    }

    private static CookieCsrfTokenRepository csrfRepository(boolean secure) {
        var tokens = new CookieCsrfTokenRepository();
        tokens.setCookieCustomizer(builder -> {
            builder.sameSite("Lax").httpOnly(true);
            if (secure) builder.secure(true);
        });
        return tokens;
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminApi(HttpSecurity http, AccountService accounts, SessionCookie cookie,
                                 @Qualifier("adminCsrfTokens") CookieCsrfTokenRepository csrfTokens, ObjectMapper json,
                                 @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        var csrf = paths.matcher(HttpMethod.GET, "/api/admin/auth/csrf");
        var login = paths.matcher(HttpMethod.POST, "/api/admin/auth/login");
        var logout = paths.matcher(HttpMethod.POST, "/api/admin/auth/logout");
        return sessionApi(http.securityMatcher("/api/admin/**"), csrfTokens, json)
                .authorizeHttpRequests(rules -> rules.requestMatchers(csrf, login, logout).permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .addFilterAfter(new SessionTokenFilter(request -> !csrf.matches(request) && !login.matches(request)
                        && !logout.matches(request), accounts, cookie, errors, true), LogoutFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain accountApi(HttpSecurity http, AccountService accounts, SessionCookie cookie,
                                   @Qualifier("csrfTokens") CookieCsrfTokenRepository csrfTokens, ObjectMapper json,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        var memberApi = paths.matcher("/api/me/**");
        var authApi = paths.matcher("/api/auth/**");
        var playApi = paths.matcher("/api/works/{workId}/playback-sessions/**");
        return sessionApi(http.securityMatcher(request -> authApi.matches(request)
                        || memberApi.matches(request) || playApi.matches(request)), csrfTokens, json)
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(playApi).permitAll()
                        .requestMatchers(paths.matcher(HttpMethod.GET, "/api/auth/csrf")).permitAll()
                        .requestMatchers(paths.matcher(HttpMethod.POST, "/api/auth/register"),
                                paths.matcher(HttpMethod.POST, "/api/auth/login"),
                                paths.matcher(HttpMethod.POST, "/api/auth/logout")).permitAll()
                        .requestMatchers(memberApi).hasRole("USER")
                        .anyRequest().denyAll())
                .addFilterAfter(new SessionTokenFilter(request -> memberApi.matches(request) || playApi.matches(request), accounts, cookie, errors, false), LogoutFilter.class)
                .build();
    }

    private static HttpSecurity sessionApi(HttpSecurity http, CookieCsrfTokenRepository csrfTokens, ObjectMapper json) throws Exception {
        return http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, error) ->
                                write(json, response, 401, Code.LOGIN_REQUIRED, "请先登录"))
                        .accessDeniedHandler((request, response, error) ->
                                write(json, response, 403, Code.FORBIDDEN, "请求未通过安全校验，请重试")));
    }

    private static void write(ObjectMapper json, HttpServletResponse response, int status, Code code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiResponse.error(code, message, List.of()));
    }
}
