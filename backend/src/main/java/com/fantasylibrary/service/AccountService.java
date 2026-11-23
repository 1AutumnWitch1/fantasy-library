package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountModels.Credentials;
import com.fantasylibrary.account.AccountModels.Profile;
import com.fantasylibrary.account.AccountModels.ProfileUpdate;
import com.fantasylibrary.account.AccountModels.Registration;
import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.persistence.mapper.AccountMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AccountService {
    public static final Duration TOKEN_LIFETIME = Duration.ofDays(7);
    private static final String ADMIN_PREFIX = "adm_";
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final ObjectProvider<AccountMapper> mappers;
    private final ObjectProvider<TransactionTemplate> transactions;
    private final PasswordEncoder passwords;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();

    public AccountService(ObjectProvider<AccountMapper> mappers, ObjectProvider<TransactionTemplate> transactions,
                          PasswordEncoder passwords) {
        this.mappers = mappers;
        this.transactions = transactions;
        this.passwords = passwords;
        dummyHash = passwords.encode(newToken());
    }

    public void register(Registration form) {
        validatePassword(form.password());
        try {
            mapper().create(normalize(form.username()), passwords.encode(form.password()), form.displayName().strip());
        } catch (DuplicateKeyException exception) {
            throw new AccountProblem(HttpStatus.CONFLICT, Code.USERNAME_TAKEN, "该账号名已被使用");
        }
    }

    public String login(Credentials form, String oldToken) {
        return signIn(form, oldToken, false);
    }

    public String loginAdmin(Credentials form, String oldToken) {
        return signIn(form, oldToken, true);
    }

    private String signIn(Credentials form, String oldToken, boolean admin) {
        validatePassword(form.password());
        var accounts = mapper();
        var login = accounts.findLogin(normalize(form.username()));
        boolean matches = passwords.matches(form.password(), login == null ? dummyHash : login.passwordHash());
        if (!matches || login == null || !requiredRole(admin).equals(login.accountRole())) {
            throw new AccountProblem(HttpStatus.UNAUTHORIZED, Code.INVALID_CREDENTIALS, "账号名或密码不正确");
        }
        String token = (admin ? ADMIN_PREFIX : "") + newToken();
        transactions.getObject().executeWithoutResult(status -> {
            if (accounts.lockActive(login.id()) == null) {
                throw new AccountProblem(HttpStatus.UNAUTHORIZED, Code.INVALID_CREDENTIALS, "账号名或密码不正确");
            }
            accounts.pruneExpired(login.id());
            accounts.createToken(digest(token), login.id(), TOKEN_LIFETIME.toSeconds());
            if (validToken(oldToken, admin)) accounts.revoke(digest(oldToken));
        });
        return token;
    }

    public AccountMapper.SessionOwner authenticate(String token) {
        return authenticate(token, false);
    }

    public AccountMapper.SessionOwner authenticateAdmin(String token) {
        return authenticate(token, true);
    }

    private AccountMapper.SessionOwner authenticate(String token, boolean admin) {
        if (!validToken(token, admin)) return null;
        var owner = mapper().authenticate(digest(token));
        return owner != null && requiredRole(admin).equals(owner.accountRole()) ? owner : null;
    }

    public void logout(String token) {
        if (validToken(token, false)) mapper().revoke(digest(token));
    }

    public void logoutAdmin(String token) {
        if (validToken(token, true)) mapper().revoke(digest(token));
    }

    private static String requiredRole(boolean admin) {
        return admin ? "ADMIN" : "MEMBER";
    }

    public Profile profile(long userId) {
        return mapper().profile(userId);
    }

    public Profile updateProfile(long userId, ProfileUpdate form) {
        var accounts = mapper();
        accounts.updateProfile(userId, form.displayName().strip(), form.bio() == null ? null : form.bio().strip());
        return accounts.profile(userId);
    }

    private AccountMapper mapper() {
        var mapper = mappers.getIfAvailable();
        if (mapper == null) throw unavailable();
        return mapper;
    }

    static AccountProblem unavailable() {
        return new AccountProblem(HttpStatus.SERVICE_UNAVAILABLE, Code.DATABASE_UNAVAILABLE, "私人书架数据库暂不可用");
    }

    private static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password.indexOf('\0') >= 0 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "密码不能包含空字符，且最多为 72 个 UTF-8 字节");
        }
    }

    private String newToken() {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private static boolean validToken(String token, boolean admin) {
        if (token == null) return false;
        if (!admin) return TOKEN_FORMAT.matcher(token).matches();
        return token.startsWith(ADMIN_PREFIX) && TOKEN_FORMAT.matcher(token.substring(ADMIN_PREFIX.length())).matches();
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required digest algorithm unavailable", exception);
        }
    }
}
