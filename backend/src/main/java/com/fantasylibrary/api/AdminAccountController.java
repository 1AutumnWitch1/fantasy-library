package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountModels.Credentials;
import com.fantasylibrary.account.AccountModels.CsrfProof;
import com.fantasylibrary.account.AccountModels.Profile;
import com.fantasylibrary.security.SessionCookie;
import com.fantasylibrary.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAccountController {
    private final AccountService accounts;
    private final SessionCookie cookie;
    private final CookieCsrfTokenRepository csrf;

    public AdminAccountController(AccountService accounts, SessionCookie cookie,
                                  @Qualifier("adminCsrfTokens") CookieCsrfTokenRepository csrf) {
        this.accounts = accounts;
        this.cookie = cookie;
        this.csrf = csrf;
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfProof> csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return ApiResponse.success(new CsrfProof(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@Valid @RequestBody Credentials form, HttpServletRequest request,
                                   HttpServletResponse response) {
        cookie.setAdmin(request, response, accounts.loginAdmin(form, cookie.readAdmin(request)));
        csrf.saveToken(null, request, response);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        accounts.logoutAdmin(cookie.readAdmin(request));
        cookie.clearAdmin(request, response);
        csrf.saveToken(null, request, response);
        return ApiResponse.success(null);
    }

    @GetMapping("/profile")
    public ApiResponse<Profile> profile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(accounts.profile(userId));
    }
}
