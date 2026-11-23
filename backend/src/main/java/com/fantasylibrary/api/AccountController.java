package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountModels.Credentials;
import com.fantasylibrary.account.AccountModels.CsrfProof;
import com.fantasylibrary.account.AccountModels.Profile;
import com.fantasylibrary.account.AccountModels.ProfileUpdate;
import com.fantasylibrary.account.AccountModels.Registration;
import com.fantasylibrary.security.SessionCookie;
import com.fantasylibrary.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {
    private final AccountService accounts;
    private final SessionCookie cookie;
    private final CookieCsrfTokenRepository csrf;

    public AccountController(AccountService accounts, SessionCookie cookie,
                             @Qualifier("csrfTokens") CookieCsrfTokenRepository csrf) {
        this.accounts = accounts;
        this.cookie = cookie;
        this.csrf = csrf;
    }

    @GetMapping("/api/auth/csrf")
    public ApiResponse<CsrfProof> csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return ApiResponse.success(new CsrfProof(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody Registration form) {
        accounts.register(form);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<Void> login(@Valid @RequestBody Credentials form, HttpServletRequest request,
                                   HttpServletResponse response) {
        cookie.set(request, response, accounts.login(form, cookie.read(request)));
        csrf.saveToken(null, request, response);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        accounts.logout(cookie.read(request));
        cookie.clear(request, response);
        csrf.saveToken(null, request, response);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/me/profile")
    public ApiResponse<Profile> profile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(accounts.profile(userId));
    }

    @PutMapping("/api/me/profile")
    public ApiResponse<Profile> update(@AuthenticationPrincipal Long userId, @Valid @RequestBody ProfileUpdate form) {
        return ApiResponse.success(accounts.updateProfile(userId, form));
    }
}
