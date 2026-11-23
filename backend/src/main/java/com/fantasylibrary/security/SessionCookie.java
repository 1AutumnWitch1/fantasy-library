package com.fantasylibrary.security;

import com.fantasylibrary.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class SessionCookie {
    private static final String NAME = "fantasy_library_session";
    private static final String ADMIN_NAME = "fantasy_library_admin_session";
    private final boolean forceSecure;

    public SessionCookie(@Value("${library.auth.secure-cookie:false}") boolean forceSecure) {
        this.forceSecure = forceSecure;
    }

    public String read(HttpServletRequest request) {
        return read(request, NAME);
    }

    public String readAdmin(HttpServletRequest request) {
        return read(request, ADMIN_NAME);
    }

    private static String read(HttpServletRequest request, String name) {
        var found = WebUtils.getCookie(request, name);
        return found == null ? null : found.getValue();
    }

    public void set(HttpServletRequest request, HttpServletResponse response, String token) {
        write(request, response, NAME, "/api", token, AccountService.TOKEN_LIFETIME);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        write(request, response, NAME, "/api", "", Duration.ZERO);
    }

    public void setAdmin(HttpServletRequest request, HttpServletResponse response, String token) {
        write(request, response, ADMIN_NAME, "/api/admin", token, AccountService.TOKEN_LIFETIME);
    }

    public void clearAdmin(HttpServletRequest request, HttpServletResponse response) {
        write(request, response, ADMIN_NAME, "/api/admin", "", Duration.ZERO);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, String name, String path,
                       String token, Duration lifetime) {
        var cookie = ResponseCookie.from(name, token)
                .path(request.getContextPath() + path)
                .httpOnly(true)
                .secure(forceSecure || request.isSecure())
                .sameSite("Lax")
                .maxAge(lifetime)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
