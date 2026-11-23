package com.fantasylibrary.security;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.persistence.mapper.AccountMapper.SessionOwner;
import com.fantasylibrary.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

final class SessionTokenFilter extends OncePerRequestFilter {
    private final RequestMatcher protectedApi;
    private final AccountService accounts;
    private final SessionCookie cookie;
    private final HandlerExceptionResolver errors;
    private final boolean admin;

    SessionTokenFilter(RequestMatcher protectedApi, AccountService accounts, SessionCookie cookie,
                       HandlerExceptionResolver errors, boolean admin) {
        this.protectedApi = protectedApi;
        this.accounts = accounts;
        this.cookie = cookie;
        this.errors = errors;
        this.admin = admin;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !protectedApi.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SessionOwner owner;
        try {
            owner = admin ? accounts.authenticateAdmin(cookie.readAdmin(request)) : accounts.authenticate(cookie.read(request));
        } catch (AccountProblem | DataAccessException exception) {
            errors.resolveException(request, response, null, exception);
            return;
        }
        if (owner != null) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    owner.id(), null, "ADMIN".equals(owner.accountRole())
                            ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
